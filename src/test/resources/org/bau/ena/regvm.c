#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <stddef.h>

// Minimal C RegVM capable of loading the serialized RegBytecode format and running simple programs.
// Notes:
// - This is a reference VM: focuses on correctness over performance.
// - Supports core ops used by tests; extend as needed.

#define MAGIC "RBVM"

typedef enum {
  OP_NOP, OP_MOV, OP_LOADI, OP_LOADR, OP_LOADS, OP_CONCAT, OP_LOAD, OP_STORE, OP_ALOADI, OP_ASTOREI, OP_ALOADT, OP_ALOADS, OP_ASTORET, OP_ASTORES, OP_ALOADR, OP_ASTORER,
  OP_JMP, OP_JMPIF, OP_CALL, OP_RET, OP_NEWARR, OP_LENI, OP_LENF, OP_LENT, OP_LENS, OP_PRINT, OP_I2R, OP_I2S, OP_R2S, OP_NEWOBJ, OP_GETF, OP_SETF, OP_ISNULL,
  OP_ADD, OP_SUB, OP_MUL, OP_DIV, OP_MOD, OP_AND, OP_OR, OP_BAND, OP_BOR, OP_XOR, OP_SHL, OP_SHR, OP_EQ, OP_NE, OP_LT, OP_LE, OP_GT, OP_GE, OP_NEG,
  OP_FADD, OP_FSUB, OP_FMUL, OP_FDIV, OP_FNEG,
  OP_FCMP, OP_TCMP
} Op;

typedef struct { int32_t op,a,b,c,imm; } Insn;

typedef struct { char *name; int32_t numRegs; int32_t codeLen; Insn *code; } Func;

typedef struct {
  int32_t version;
  int32_t textCount; char **texts;
  int32_t realCount; double *reals;
  int32_t typeCount; // skipped for now
  int32_t funcCount; Func *funcs;
} Bytecode;

static int read_i32(FILE *f, int32_t *out){ uint8_t b[4]; if (fread(b,1,4,f)!=4) return 0; *out = (b[0]<<24)|(b[1]<<16)|(b[2]<<8)|b[3]; return 1; }
static int read_f64(FILE *f, double *out){ uint64_t u=0; for (int i=0;i<8;i++){ int c=fgetc(f); if (c==EOF) return 0; u=(u<<8)|((uint8_t)c); } memcpy(out,&u,8); return 1; }
static int read_str(FILE *f, char **out){ int32_t n; if (!read_i32(f,&n)) return 0; char *s=(char*)malloc(n+1); if ((int)fread(s,1,n,f)!=n){ free(s); return 0; } s[n]='\0'; *out=s; return 1; }

static int load_bytecode(const char *path, Bytecode *bc){
  memset(bc,0,sizeof(*bc)); FILE *f=fopen(path,"rb"); if (!f) return 0; char m[4]; if (fread(m,1,4,f)!=4 || memcmp(m,MAGIC,4)!=0){ fclose(f); return 0; }
  if (!read_i32(f,&bc->version)) { fclose(f); return 0; }
  if (!read_i32(f,&bc->textCount)) { fclose(f); return 0; }
  bc->texts=(char**)calloc(bc->textCount,sizeof(char*)); for (int i=0;i<bc->textCount;i++) if (!read_str(f,&bc->texts[i])) { fclose(f); return 0; }
  if (!read_i32(f,&bc->realCount)) { fclose(f); return 0; }
  bc->reals=(double*)calloc(bc->realCount,sizeof(double)); for (int i=0;i<bc->realCount;i++) if (!read_f64(f,&bc->reals[i])) { fclose(f); return 0; }
  if (!read_i32(f,&bc->typeCount)) { fclose(f); return 0; }
  // Skip types for now
  for (int t=0;t<bc->typeCount;t++){
    char *tn; if (!read_str(f,&tn)) { fclose(f); return 0; } free(tn);
    int32_t fcnt; if (!read_i32(f,&fcnt)) { fclose(f); return 0; }
    for (int j=0;j<fcnt;j++){ char *fn; if (!read_str(f,&fn)) { fclose(f); return 0; } free(fn); char *ft; if (!read_str(f,&ft)) { fclose(f); return 0; } free(ft); }
  }
  if (!read_i32(f,&bc->funcCount)) { fclose(f); return 0; }
  bc->funcs=(Func*)calloc(bc->funcCount,sizeof(Func));
  for (int i=0;i<bc->funcCount;i++){
    if (!read_str(f,&bc->funcs[i].name)) { fclose(f); return 0; }
    if (!read_i32(f,&bc->funcs[i].numRegs)) { fclose(f); return 0; }
    if (!read_i32(f,&bc->funcs[i].codeLen)) { fclose(f); return 0; }
    bc->funcs[i].code=(Insn*)calloc(bc->funcs[i].codeLen,sizeof(Insn));
    for (int k=0;k<bc->funcs[i].codeLen;k++){
      if (!read_i32(f,&bc->funcs[i].code[k].op)) { fclose(f); return 0; }
      if (!read_i32(f,&bc->funcs[i].code[k].a)) { fclose(f); return 0; }
      if (!read_i32(f,&bc->funcs[i].code[k].b)) { fclose(f); return 0; }
      if (!read_i32(f,&bc->funcs[i].code[k].c)) { fclose(f); return 0; }
      if (!read_i32(f,&bc->funcs[i].code[k].imm)) { fclose(f); return 0; }
    }
  }
  fclose(f); return 1;
}

#ifndef REGVM_NO_TAGS
#define SET_TAG(V,T) ((V).tag = (T))
#define CHECK_TAG(V,T) ((V).tag == (T))
#else
#define SET_TAG(V,T) ((void)0)
#define CHECK_TAG(V,T)    (0)  /* never trust tag comparisons in no-tag mode */
#endif


// Simple generic Value representation: tagged union
typedef enum { VT_INT, VT_REAL, VT_TEXT, VT_ARRI, VT_ARRR, VT_NULL } VTag;
typedef struct {
#ifndef REGVM_NO_TAGS
  VTag tag;
#endif
  union { int64_t i; double r; char *s; void *p; } u;
} Value;

static int is_truthy(Value v){
  //if (CHECK_TAG(v, VT_TEXT)) return v.u.s && v.u.s[0] != '\0';
  //if (CHECK_TAG(v, VT_PTR)) return 1;
  //if (CHECK_TAG(v, VT_NULL)) return 0;
  return (v.u.i != 0) || (v.u.r != 0.0);
}

static int64_t to_int(Value v){
  return v.u.i;
}
static double to_real(Value v){
  return v.u.r;
}

static int find_func(Bytecode *bc, const char *name){ for (int i=0;i<bc->funcCount;i++) if (strcmp(bc->funcs[i].name,name)==0) return i; return -1; }

static Value exec(Bytecode *bc, int fidx, Value *args, int argc);

static Value run(Bytecode *bc, const char *entry){
  int idx=find_func(bc, entry);
  if (idx<0) { idx=find_func(bc,"main"); if (idx<0) idx=find_func(bc,"<toplevel>"); }
  Value none;
  return exec(bc, idx, NULL, 0);
}

// Simple globals
static Value *G = NULL; static int Glen = 0;
static Value get_global(int idx){
  if (idx<0) {
    Value v; SET_TAG(v, VT_NULL); v.u.i = 0; return v;
  }
  if (idx>=Glen){
    int newLen = idx+1;
    G = (Value*)realloc(G, newLen*sizeof(Value));
    for (int i=Glen;i<newLen;i++){
      G[i].u.i=0; SET_TAG(G[i], VT_NULL);
    }
    Glen=newLen;
  }
  return G[idx];
}
// ===== Reference counting support =====
typedef struct { int32_t rc; int32_t len; char data[]; } StrHdr;
static size_t g_heap_objects = 0;

static char* str_new_copy(const char *src){
  size_t n = src ? strlen(src) : 0;
  StrHdr *h = (StrHdr*)malloc(sizeof(StrHdr) + n + 1);
  h->rc = 1; h->len = (int32_t)n;
  if (n>0) memcpy(h->data, src, n);
  h->data[n] = '\0';
  g_heap_objects++;
  return h->data;
}
static inline StrHdr* str_hdr_from_data(char *s){ return (StrHdr*)((char*)s - offsetof(StrHdr, data)); }
static void str_inc(char *s){ if (!s) return; StrHdr *h = str_hdr_from_data(s); h->rc++; }
static void str_dec(char *s){ if (!s) return; StrHdr *h = str_hdr_from_data(s); if (--h->rc == 0){ g_heap_objects--; free(h); } }

// minimal typed array heap: int and real
typedef struct { int32_t id; int32_t len; int64_t *data; int32_t rc; } IntArr;
typedef struct { int32_t id; int32_t len; double  *data; int32_t rc; } RealArr;

IntArr *IA=NULL; int IAcount=0; int nextArrId=1;
RealArr *RA=NULL; int RAcount=0;

IntArr* getIA(int idx){ if (idx < 0 || idx >= IAcount) return NULL; return &IA[idx]; }
RealArr* getRA(int idx){ if (idx < 0 || idx >= RAcount) return NULL; return &RA[idx]; }

static void arr_inc_i(int id){ IntArr *ia = getIA(id); if (ia) ia->rc++; }
static void arr_dec_i(int id){ IntArr *ia = getIA(id); if (!ia) return; if (--ia->rc == 0){ if (ia->data) free(ia->data); ia->data=NULL; ia->len=0; g_heap_objects--; } }
static void arr_inc_r(int id){ RealArr *ra = getRA(id); if (ra) ra->rc++; }
static void arr_dec_r(int id){ RealArr *ra = getRA(id); if (!ra) return; if (--ra->rc == 0){ if (ra->data) free(ra->data); ra->data=NULL; ra->len=0; g_heap_objects--; } }

#ifndef REGVM_NO_TAGS
static void clear_value(Value *v){
  if (!v) return;
  if (CHECK_TAG(*v, VT_TEXT) && v->u.s){ str_dec(v->u.s); }
  else if (CHECK_TAG(*v, VT_ARRI)) { int id = (int)(intptr_t)v->u.p; if (id) arr_dec_i(id); }
  else if (CHECK_TAG(*v, VT_ARRR)) { int id = (int)(intptr_t)v->u.p; if (id) arr_dec_r(id); }
  v->u.i = 0; SET_TAG((*v), VT_NULL);
}

static void retain_value(Value v){
  if (CHECK_TAG(v, VT_TEXT) && v.u.s) { str_inc(v.u.s); }
  else if (CHECK_TAG(v, VT_ARRI)) { int id = (int)(intptr_t)v.u.p; if (id) arr_inc_i(id); }
  else if (CHECK_TAG(v, VT_ARRR)) { int id = (int)(intptr_t)v.u.p; if (id) arr_inc_r(id); }
}

static void assign_value(Value *dst, Value src){
  clear_value(dst);
  *dst = src;
  retain_value(*dst);
}
#else
// In no-tag mode, skip reference counting to avoid interpreting registers as wrong kinds
static void clear_value(Value *v){ if (!v) return; v->u.i = 0; }
static void retain_value(Value v){ (void)v; }
static void assign_value(Value *dst, Value src){ *dst = src; }
#endif

static void set_global(int idx, Value v){
  if (idx<0) return;
  if (idx>=Glen){
    int newLen = idx+1;
    G = (Value*)realloc(G, newLen*sizeof(Value));
    for (int i=Glen;i<newLen;i++){
      G[i].u.i=0;
      SET_TAG(G[i], VT_NULL);
    }
    Glen=newLen;
  }
  assign_value(&G[idx], v);
}

static void release_registers(Value *R, int n, int retIdx){
  for (int i=0;i<n;i++){ if (i==retIdx) continue; clear_value(&R[i]); }
}

static Value exec(Bytecode *bc, int fidx, Value *args, int argc){
  Func *f=&bc->funcs[fidx];
  Value *R=(Value*)calloc(f->numRegs, sizeof(Value));
  for (int i=0;i<f->numRegs;i++){ SET_TAG(R[i], VT_NULL); R[i].u.i=0; }
  for (int i=0;i<argc;i++) assign_value(&R[i+1], args[i]);
  int pc=0; Value ret; 
  //ret.tag=VT_NULL;

#ifdef REGVM_THREADED
  #define DISPATCH() do { if (pc >= f->codeLen) { free(R); return ret; } in = f->code[pc++]; goto *jt[in.op]; } while(0)
  Insn in;
  static void *jt[] = {
    [OP_NOP]=&&L_NOP,
    [OP_NEWARR]=&&L_NEWARR,
    [OP_ALOADR]=&&L_ALOADR, [OP_ALOADT]=&&L_ALOADT, [OP_ALOADS]=&&L_ALOADS, [OP_ALOADI]=&&L_ALOADI,
    [OP_ASTORER]=&&L_ASTORER, [OP_ASTORET]=&&L_ASTORET, [OP_ASTORES]=&&L_ASTORES, [OP_ASTOREI]=&&L_ASTOREI,
    [OP_LENI]=&&L_LENI, [OP_LENF]=&&L_LENF, [OP_LENT]=&&L_LENT, [OP_LENS]=&&L_LENS,
    [OP_MOV]=&&L_MOV, [OP_LOADI]=&&L_LOADI, [OP_LOADR]=&&L_LOADR, [OP_LOADS]=&&L_LOADS,
    [OP_LOAD]=&&L_LOAD, [OP_STORE]=&&L_STORE,
    [OP_ADD]=&&L_ADD, [OP_SUB]=&&L_SUB, [OP_MUL]=&&L_MUL, [OP_DIV]=&&L_DIV, [OP_MOD]=&&L_MOD,
    [OP_AND]=&&L_AND, [OP_OR]=&&L_OR, [OP_BAND]=&&L_BAND, [OP_BOR]=&&L_BOR, [OP_XOR]=&&L_XOR,
    [OP_SHL]=&&L_SHL, [OP_SHR]=&&L_SHR, [OP_NEG]=&&L_NEG,
    [OP_EQ]=&&L_EQ, [OP_NE]=&&L_NE, [OP_LT]=&&L_LT, [OP_LE]=&&L_LE, [OP_GT]=&&L_GT, [OP_GE]=&&L_GE,
    [OP_FADD]=&&L_FADD, [OP_FSUB]=&&L_FSUB, [OP_FMUL]=&&L_FMUL, [OP_FDIV]=&&L_FDIV, [OP_FNEG]=&&L_FNEG,
    [OP_FCMP]=&&L_FCMP, [OP_TCMP]=&&L_TCMP, [OP_CONCAT]=&&L_CONCAT,
    [OP_PRINT]=&&L_PRINT, [OP_JMP]=&&L_JMP, [OP_JMPIF]=&&L_JMPIF, [OP_CALL]=&&L_CALL, [OP_RET]=&&L_RET,
    [OP_I2R]=&&L_I2R, [OP_I2S]=&&L_I2S, [OP_R2S]=&&L_R2S,
    [OP_ISNULL]=&&L_ISNULL,
  };
  DISPATCH();
L_NOP: { /* no-op */ } DISPATCH();
// Hot path labels placed early for better locality under REGVM_THREADED
L_LOADI: { clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i=in.imm; } DISPATCH();
L_MOV: { assign_value(&R[in.a], R[in.b]); } DISPATCH();
L_JMP: { pc = in.a; } DISPATCH();
L_JMPIF: { if (R[in.a].u.i != 0) pc = in.b; } DISPATCH();
L_ADD: { R[in.a].u.i = R[in.b].u.i + R[in.c].u.i; } DISPATCH();
L_SUB: { R[in.a].u.i = R[in.b].u.i - R[in.c].u.i; } DISPATCH();
L_ALOADI: {
    int idx = (int)R[in.c].u.i; int id = (int)(intptr_t)R[in.b].u.p; IntArr *ia = getIA(id);
    if (ia){ if (idx<0 || idx>=ia->len){ fprintf(stderr, "index OOB\n"); exit(1);} clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i=ia->data[idx]; } else { fprintf(stderr, "ALOADI: bad array index %d\n", id); exit(1);} }
  DISPATCH();
L_ASTOREI: { int idx = (int)R[in.b].u.i; int id = (int)(intptr_t)R[in.a].u.p; IntArr *ia = getIA(id); if (ia){ if (idx<0 || idx>=ia->len){ fprintf(stderr, "index OOB\n"); exit(1);} ia->data[idx] = R[in.c].u.i; } else { fprintf(stderr, "ASTOREI: bad array index %d\n", id); exit(1);} } DISPATCH();
L_NEWARR: {
    int type = in.b; int len = (int)R[in.c].u.i;
    if (type==1){ IA = (IntArr*)realloc(IA, (IAcount+1)*sizeof(IntArr)); IA[IAcount].id = IAcount; IA[IAcount].len = len; IA[IAcount].data = (int64_t*)calloc(len, sizeof(int64_t)); IA[IAcount].rc=1; clear_value(&R[in.a]); SET_TAG(R[in.a], VT_ARRI); R[in.a].u.p = (void*)(intptr_t)IAcount; IAcount++; g_heap_objects++; }
    else if (type==2){ RA = (RealArr*)realloc(RA, (RAcount+1)*sizeof(RealArr)); RA[RAcount].id = RAcount; RA[RAcount].len = len; RA[RAcount].data = (double*)calloc(len, sizeof(double)); RA[RAcount].rc=1; clear_value(&R[in.a]); SET_TAG(R[in.a], VT_ARRR); R[in.a].u.p = (void*)(intptr_t)RAcount; RAcount++; g_heap_objects++; }
    else { fprintf(stderr, "NEWARR: unsupported type %d\n", type); exit(1); }
  } DISPATCH();
L_ALOADR: {
    int idx = (int)R[in.c].u.i; int id = (int)(intptr_t)R[in.b].u.p;
    RealArr *ra = getRA(id); if (ra){ if (idx<0 || idx>=ra->len){ fprintf(stderr, "index OOB\n"); exit(1);} clear_value(&R[in.a]); SET_TAG(R[in.a], VT_REAL); R[in.a].u.r=ra->data[idx]; } else { fprintf(stderr, "ALOADR: bad array index %d\n", id); exit(1);} }
  DISPATCH();
L_ALOADT: { int idx = (int)R[in.c].u.i; (void)idx; fprintf(stderr, "ALOADT: typed text arrays not fully modeled in C VM sample\n"); exit(1);} 
L_ALOADS: { int idx = (int)R[in.c].u.i; (void)idx; fprintf(stderr, "ALOADS: typed struct arrays not fully modeled in C VM sample\n"); exit(1);} 
L_ASTORER: { int idx = (int)R[in.b].u.i; int id = (int)(intptr_t)R[in.a].u.p; RealArr *ra = getRA(id); if (ra){ if (idx<0 || idx>=ra->len){ fprintf(stderr, "index OOB\n"); exit(1);} ra->data[idx] = R[in.c].u.r; } else { fprintf(stderr, "ASTORER: bad array index %d\n", id); exit(1);} } DISPATCH();
L_ASTORET: { fprintf(stderr, "ASTORET: typed text arrays not fully modeled in C VM sample\n"); exit(1);} 
L_ASTORES: { fprintf(stderr, "ASTORES: typed struct arrays not fully modeled in C VM sample\n"); exit(1);} 
L_LENI: { int id = (int)(intptr_t)R[in.b].u.p; IntArr *ia = getIA(id); clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i = ia? ia->len : 0; } DISPATCH();
L_LENF: { int id = (int)(intptr_t)R[in.b].u.p; RealArr *ra = getRA(id); clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i = ra? ra->len : 0; } DISPATCH();
L_LENT: { const char *s = R[in.b].u.s; clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i = s ? (int)strlen(s) : 0; } DISPATCH();
L_LENS: { SET_TAG(R[in.a], VT_INT); R[in.a].u.i=0; } DISPATCH();
L_LOADR: { SET_TAG(R[in.a], VT_REAL); R[in.a].u.r=bc->reals[in.imm]; } DISPATCH();
L_LOADS: {
#ifdef REGVM_NO_TAGS
    R[in.a].u.s = bc->texts[in.imm];
#else
    clear_value(&R[in.a]); SET_TAG(R[in.a], VT_TEXT); R[in.a].u.s=str_new_copy(bc->texts[in.imm]);
#endif
  } DISPATCH();
L_LOAD: { Value tmp = get_global(in.b); assign_value(&R[in.a], tmp); } DISPATCH();
L_STORE: { set_global(in.a, R[in.b]); } DISPATCH();
L_MUL: { R[in.a].u.i = R[in.b].u.i * R[in.c].u.i; } DISPATCH();
L_DIV: { clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); int64_t b = R[in.c].u.i; R[in.a].u.i = b==0?0:(R[in.b].u.i / b); } DISPATCH();
L_MOD: { clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); int64_t b = R[in.c].u.i; R[in.a].u.i = b==0?0:(R[in.b].u.i % b); } DISPATCH();
L_AND: { clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i = ((R[in.b].u.i != 0) && (R[in.c].u.i != 0))?1:0; } DISPATCH();
L_OR:  { clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i = ((R[in.b].u.i != 0) || (R[in.c].u.i != 0))?1:0; } DISPATCH();
L_BAND:{ clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i = R[in.b].u.i & R[in.c].u.i; } DISPATCH();
L_BOR: { clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i = R[in.b].u.i | R[in.c].u.i; } DISPATCH();
L_XOR: { clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i = R[in.b].u.i ^ R[in.c].u.i; } DISPATCH();
L_SHL: { clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i = (R[in.b].u.i << R[in.c].u.i); } DISPATCH();
L_SHR: { clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i = (int64_t)(((uint64_t)R[in.b].u.i) >> R[in.c].u.i); } DISPATCH();
L_NEG: { clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i = -R[in.b].u.i; } DISPATCH();
L_EQ:  { clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i=(R[in.b].u.i==R[in.c].u.i)?1:0; } DISPATCH();
L_NE:  { clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i=(R[in.b].u.i!=R[in.c].u.i)?1:0; } DISPATCH();
L_LT:  { clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i=(R[in.b].u.i<R[in.c].u.i)?1:0; } DISPATCH();
L_LE:  { clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i=(R[in.b].u.i<=R[in.c].u.i)?1:0; } DISPATCH();
L_GT:  { clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i=(R[in.b].u.i>R[in.c].u.i)?1:0; } DISPATCH();
L_GE:  { clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i=(R[in.b].u.i>=R[in.c].u.i)?1:0; } DISPATCH();
L_FADD:{ clear_value(&R[in.a]); SET_TAG(R[in.a], VT_REAL); R[in.a].u.r = R[in.b].u.r + R[in.c].u.r; } DISPATCH();
L_FSUB:{ clear_value(&R[in.a]); SET_TAG(R[in.a], VT_REAL); R[in.a].u.r = R[in.b].u.r - R[in.c].u.r; } DISPATCH();
L_FMUL:{ clear_value(&R[in.a]); SET_TAG(R[in.a], VT_REAL); R[in.a].u.r = R[in.b].u.r * R[in.c].u.r; } DISPATCH();
L_FDIV:{ clear_value(&R[in.a]); SET_TAG(R[in.a], VT_REAL); R[in.a].u.r = R[in.b].u.r / R[in.c].u.r; } DISPATCH();
L_FNEG:{ clear_value(&R[in.a]); SET_TAG(R[in.a], VT_REAL); R[in.a].u.r = -R[in.b].u.r; } DISPATCH();
L_FCMP:{ clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); double da=R[in.b].u.r, dc=R[in.c].u.r; R[in.a].u.i = (da<dc)?-1:(da>dc)?1:0; } DISPATCH();
L_TCMP:{ const char *sa = R[in.b].u.s ? R[in.b].u.s : ""; const char *sb = R[in.c].u.s ? R[in.c].u.s : ""; int cmp = strcmp(sa, sb); R[in.a].u.i = (cmp<0)?-1:(cmp>0)?1:0; } DISPATCH();
L_CONCAT: {
    const char *sa = (CHECK_TAG(R[in.b], VT_TEXT) && R[in.b].u.s)? R[in.b].u.s : "";
    const char *sb = (CHECK_TAG(R[in.c], VT_TEXT) && R[in.c].u.s)? R[in.c].u.s : "";
    size_t la=strlen(sa), lb=strlen(sb);
    StrHdr *h2 = (StrHdr*)malloc(sizeof(StrHdr)+la+lb+1);
    h2->rc=1; h2->len=(int32_t)(la+lb);
    memcpy(h2->data, sa, la); memcpy(h2->data+la, sb, lb); h2->data[la+lb]='\0';
    g_heap_objects++;
    clear_value(&R[in.a]); SET_TAG(R[in.a], VT_TEXT); R[in.a].u.s=h2->data;
  } DISPATCH();
L_PRINT: {
#ifdef REGVM_NO_TAGS
    if (R[in.a].u.s) printf("%s\n", R[in.a].u.s); else printf("%lld\n", (long long)R[in.a].u.i);
#else
    printf("%s\n", R[in.a].u.s ? R[in.a].u.s : "");
#endif
  } DISPATCH();
L_I2R: { clear_value(&R[in.a]); SET_TAG(R[in.a], VT_REAL); R[in.a].u.r = (double)R[in.b].u.i; } DISPATCH();
L_I2S: {
#ifdef REGVM_NO_TAGS
    static char buf[32]; snprintf(buf,sizeof(buf),"%lld", (long long)R[in.b].u.i); R[in.a].u.s = buf;
#else
    char buf[32]; snprintf(buf,sizeof(buf),"%lld", (long long)R[in.b].u.i); clear_value(&R[in.a]); SET_TAG(R[in.a], VT_TEXT); R[in.a].u.s=str_new_copy(buf);
#endif
  } DISPATCH();
L_R2S: {
#ifdef REGVM_NO_TAGS
    static char buf[32]; snprintf(buf,sizeof(buf),"%g", R[in.b].u.r); R[in.a].u.s = buf;
#else
    char buf[32]; snprintf(buf,sizeof(buf),"%g", R[in.b].u.r); clear_value(&R[in.a]); SET_TAG(R[in.a], VT_TEXT); R[in.a].u.s=str_new_copy(buf);
#endif
  } DISPATCH();
L_ISNULL: { clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); int isnull = (CHECK_TAG(R[in.b], VT_NULL) || (CHECK_TAG(R[in.b], VT_INT) && R[in.b].u.i==0)); R[in.a].u.i = isnull ? 1 : 0; } DISPATCH();
L_CALL: { Value *argv=(Value*)calloc(in.c, sizeof(Value)); for (int i=0;i<in.c;i++) argv[i]=R[i+1]; Value rv=exec(bc, in.b, argv, in.c); free(argv); assign_value(&R[in.a], rv); } DISPATCH();
L_RET: {
    int retIdx = (in.a==0) ? -1 : in.a;
    if (in.a==0) { Value v; ret = v; SET_TAG(ret, VT_NULL); }
    else { ret = R[in.a]; }
    release_registers(R, f->numRegs, retIdx);
    free(R); return ret;
  }
#else
  while (pc < f->codeLen){
    Insn in=f->code[pc++];
    switch ((Op)in.op){
      case OP_NOP: break;
      case OP_NEWARR: {
        int type = in.b; int len = (int)R[in.c].u.i;
        if (type==1){ // int
          IA = (IntArr*)realloc(IA, (IAcount+1)*sizeof(IntArr));
          IA[IAcount].id = IAcount; IA[IAcount].len = len; IA[IAcount].data = (int64_t*)calloc(len, sizeof(int64_t)); IA[IAcount].rc=1;
          clear_value(&R[in.a]); SET_TAG(R[in.a], VT_ARRI); R[in.a].u.p = (void*)(intptr_t)IAcount; IAcount++; g_heap_objects++;
        } else if (type==2){ // real
          RA = (RealArr*)realloc(RA, (RAcount+1)*sizeof(RealArr));
          RA[RAcount].id = RAcount; RA[RAcount].len = len; RA[RAcount].data = (double*)calloc(len, sizeof(double)); RA[RAcount].rc=1;
          clear_value(&R[in.a]); SET_TAG(R[in.a], VT_ARRR); R[in.a].u.p = (void*)(intptr_t)RAcount; RAcount++; g_heap_objects++;
        } else {
          fprintf(stderr, "NEWARR: unsupported type %d\n", type);
          exit(1);
        }
      } break;
      
      case OP_ALOADR: {
        int idx = (int)R[in.c].u.i; int id = (int)(intptr_t)R[in.b].u.p;
        RealArr *ra = getRA(id); if (ra){ if (idx<0 || idx>=ra->len){ fprintf(stderr, "index OOB\n"); exit(1);} clear_value(&R[in.a]); SET_TAG(R[in.a], VT_REAL); R[in.a].u.r=ra->data[idx]; break; }
        fprintf(stderr, "ALOADR: bad array index %d\n", id); exit(1);
      } break;
      case OP_ALOADT: {
        int idx = (int)R[in.c].u.i; int id = (int)(intptr_t)R[in.b].u.p;
        // only text arrays; reuse existing text len structure
        // In this minimal VM, text arrays are not fully modeled separately; treat as error if not an int array
        fprintf(stderr, "ALOADT: typed text arrays not fully modeled in C VM sample\n"); exit(1);
      } break;
      case OP_ALOADS: {
        int idx = (int)R[in.c].u.i; int id = (int)(intptr_t)R[in.b].u.p;
        fprintf(stderr, "ALOADS: typed struct arrays not fully modeled in C VM sample\n"); exit(1);
      } break;
      case OP_ALOADI: {
        int idx = (int)R[in.c].u.i; int id = (int)(intptr_t)R[in.b].u.p;
        IntArr *ia = getIA(id); if (ia){ if (idx<0 || idx>=ia->len){ fprintf(stderr, "index OOB\n"); exit(1);} clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i=ia->data[idx]; break; }
        fprintf(stderr, "ALOADI: bad array index %d\n", id); exit(1);
      } break;
      
      case OP_ASTORER: {
        int idx = (int)R[in.b].u.i; int id = (int)(intptr_t)R[in.a].u.p;
        RealArr *ra = getRA(id); if (ra){ if (idx<0 || idx>=ra->len){ fprintf(stderr, "index OOB\n"); exit(1);} ra->data[idx] = R[in.c].u.r; break; }
        fprintf(stderr, "ASTORER: bad array index %d\n", id); exit(1);
      } break;
      case OP_ASTORET: {
        fprintf(stderr, "ASTORET: typed text arrays not fully modeled in C VM sample\n"); exit(1);
      } break;
      case OP_ASTORES: {
        fprintf(stderr, "ASTORES: typed struct arrays not fully modeled in C VM sample\n"); exit(1);
      } break;
      case OP_ASTOREI: {
        int idx = (int)R[in.b].u.i; int id = (int)(intptr_t)R[in.a].u.p;
        IntArr *ia = getIA(id); if (ia){ if (idx<0 || idx>=ia->len){ fprintf(stderr, "index OOB\n"); exit(1);} ia->data[idx] = R[in.c].u.i; break; }
        fprintf(stderr, "ASTOREI: bad array index %d\n", id); exit(1);
      } break;
      case OP_LENI: {
        int id = (int)(intptr_t)R[in.b].u.p; IntArr *ia = getIA(id); clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i = ia? ia->len : 0; }
      break;
      case OP_LENF: {
        int id = (int)(intptr_t)R[in.b].u.p; RealArr *ra = getRA(id); clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i = ra? ra->len : 0; }
      break;
      case OP_LENT: {
        const char *s = R[in.b].u.s; clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i = s ? (int)strlen(s) : 0;
      } break;
      case OP_LENS: {
        // For user-type arrays not modeled here; return 0
        SET_TAG(R[in.a], VT_INT); R[in.a].u.i=0;
      } break;
      case OP_MOV: assign_value(&R[in.a], R[in.b]); break;
      case OP_LOADI: clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i=in.imm; break;
      case OP_LOADR: clear_value(&R[in.a]); SET_TAG(R[in.a], VT_REAL); R[in.a].u.r=bc->reals[in.imm]; break;
      case OP_LOADS: clear_value(&R[in.a]); SET_TAG(R[in.a], VT_TEXT); R[in.a].u.s=str_new_copy(bc->texts[in.imm]); break;
      case OP_LOAD: { Value tmp=get_global(in.b); assign_value(&R[in.a], tmp); } break;
      case OP_STORE: set_global(in.a, R[in.b]); break;
      case OP_ADD: clear_value(&R[in.a]); R[in.a].u.i = R[in.b].u.i + R[in.c].u.i; SET_TAG(R[in.a], VT_INT); break;
      case OP_SUB: clear_value(&R[in.a]); R[in.a].u.i = R[in.b].u.i - R[in.c].u.i; SET_TAG(R[in.a], VT_INT); break;
      case OP_MUL: clear_value(&R[in.a]); R[in.a].u.i = R[in.b].u.i * R[in.c].u.i; SET_TAG(R[in.a], VT_INT); break;
      case OP_DIV: clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); { int64_t b = R[in.c].u.i; R[in.a].u.i = b==0?0:(R[in.b].u.i / b); } break;
      case OP_MOD: clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); { int64_t b = R[in.c].u.i; R[in.a].u.i = b==0?0:(R[in.b].u.i % b); } break;
      case OP_AND: clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i = ((R[in.b].u.i != 0) && (R[in.c].u.i != 0))?1:0; break;
      case OP_OR:  clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i = ((R[in.b].u.i != 0) || (R[in.c].u.i != 0))?1:0; break;
      case OP_BAND: clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i = R[in.b].u.i & R[in.c].u.i; break;
      case OP_BOR:  clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i = R[in.b].u.i | R[in.c].u.i; break;
      case OP_XOR:  clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i = R[in.b].u.i ^ R[in.c].u.i; break;
      case OP_SHL:  clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i = (R[in.b].u.i << R[in.c].u.i); break;
      case OP_SHR:  clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i = (int64_t)(((uint64_t)R[in.b].u.i) >> R[in.c].u.i); break;
      case OP_NEG: clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i = -R[in.b].u.i; break;
      case OP_EQ: clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i=(R[in.b].u.i==R[in.c].u.i)?1:0; break;
      case OP_NE: clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i=(R[in.b].u.i!=R[in.c].u.i)?1:0; break;
      case OP_LT: clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i=(R[in.b].u.i<R[in.c].u.i)?1:0; break;
      case OP_LE: clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i=(R[in.b].u.i<=R[in.c].u.i)?1:0; break;
      case OP_GT: clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i=(R[in.b].u.i>R[in.c].u.i)?1:0; break;
      case OP_GE: clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); R[in.a].u.i=(R[in.b].u.i>=R[in.c].u.i)?1:0; break;
      case OP_FADD: clear_value(&R[in.a]); SET_TAG(R[in.a], VT_REAL); R[in.a].u.r = R[in.b].u.r + R[in.c].u.r; break;
      case OP_FSUB: clear_value(&R[in.a]); SET_TAG(R[in.a], VT_REAL); R[in.a].u.r = R[in.b].u.r - R[in.c].u.r; break;
      case OP_FMUL: clear_value(&R[in.a]); SET_TAG(R[in.a], VT_REAL); R[in.a].u.r = R[in.b].u.r * R[in.c].u.r; break;
      case OP_FDIV: clear_value(&R[in.a]); SET_TAG(R[in.a], VT_REAL); R[in.a].u.r = R[in.b].u.r / R[in.c].u.r; break;
      case OP_FNEG: clear_value(&R[in.a]); SET_TAG(R[in.a], VT_REAL); R[in.a].u.r = -R[in.b].u.r; break;
      case OP_FCMP: clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT); { double da=R[in.b].u.r, dc=R[in.c].u.r; R[in.a].u.i = (da<dc)?-1:(da>dc)?1:0; } break;
      case OP_TCMP: 
        // R[in.a].tag=VT_INT; 
        {
            const char *sa = R[in.b].u.s ? R[in.b].u.s : "";
            const char *sb = R[in.c].u.s ? R[in.c].u.s : "";
        clear_value(&R[in.a]); int cmp = strcmp(sa, sb); R[in.a].u.i = (cmp<0)?-1:(cmp>0)?1:0; SET_TAG(R[in.a], VT_INT);
        }
        break;
      case OP_CONCAT: {
        const char *sa = (CHECK_TAG(R[in.b], VT_TEXT) && R[in.b].u.s)? R[in.b].u.s : "";
        const char *sb = (CHECK_TAG(R[in.c], VT_TEXT) && R[in.c].u.s)? R[in.c].u.s : "";
        size_t la=strlen(sa), lb=strlen(sb);
        StrHdr *h = (StrHdr*)malloc(sizeof(StrHdr)+la+lb+1); h->rc=1; h->len=(int32_t)(la+lb);
        memcpy(h->data, sa, la); memcpy(h->data+la, sb, lb); h->data[la+lb]='\0'; g_heap_objects++;
        clear_value(&R[in.a]); SET_TAG(R[in.a], VT_TEXT); R[in.a].u.s=h->data;
      } break;
      case OP_PRINT:
        printf("%s\n", R[in.a].u.s ? R[in.a].u.s : "");
        break;
      case OP_I2S: {
        char buf[32]; snprintf(buf,sizeof(buf),"%lld", (long long)R[in.b].u.i); clear_value(&R[in.a]); SET_TAG(R[in.a], VT_TEXT); R[in.a].u.s=str_new_copy(buf); }
      break;
      case OP_R2S: {
        char buf[32]; snprintf(buf,sizeof(buf),"%g", R[in.b].u.r); clear_value(&R[in.a]); SET_TAG(R[in.a], VT_TEXT); R[in.a].u.s=str_new_copy(buf); }
        break;
      case OP_I2R: {
        clear_value(&R[in.a]); SET_TAG(R[in.a], VT_REAL); R[in.a].u.r = (double)R[in.b].u.i;
      } break;
      case OP_JMP: pc = in.a; break;
      case OP_JMPIF: if (R[in.a].u.i != 0) pc = in.b; break;
      case OP_ISNULL: {
        clear_value(&R[in.a]); SET_TAG(R[in.a], VT_INT);
        int isnull = (CHECK_TAG(R[in.b], VT_NULL) || (CHECK_TAG(R[in.b], VT_INT) && R[in.b].u.i==0));
        R[in.a].u.i = isnull ? 1 : 0;
      } break;
      case OP_CALL: {
        Value *argv=(Value*)calloc(in.c, sizeof(Value));
        for (int i=0;i<in.c;i++) argv[i]=R[i+1];
        Value rv=exec(bc, in.b, argv, in.c); free(argv);
        assign_value(&R[in.a], rv);
      } break;
      case OP_RET: {
        int retIdx = (in.a==0) ? -1 : in.a;
        if (in.a==0) {
            Value v;
            ret = v;
            SET_TAG(ret, VT_NULL);
        } else {
            ret = R[in.a];
        }
        release_registers(R, f->numRegs, retIdx);
        free(R); 
        return ret;
      }
      default: fprintf(stderr, "Unsupported opcode %d\n", in.op); free(R); exit(1);
    }
  }
#endif

  free(R); return ret;
}

//#ifdef REGVM_MAIN
static void cleanup_globals(){
  for (int i=0;i<Glen;i++) clear_value(&G[i]);
  free(G); G=NULL; Glen=0;
}

int main(int argc, char **argv){
  if (argc<2){ fprintf(stderr, "usage: %s file.rbvm\n", argv[0]); return 1; }
  Bytecode bc; if (!load_bytecode(argv[1], &bc)){ fprintf(stderr, "failed to load %s\n", argv[1]); return 1; }
  run(&bc, "main");
  cleanup_globals();
  #ifndef REGVM_NO_TAGS
  if (g_heap_objects != 0){ fprintf(stderr, "memory leak: %zu objects still alive\n", g_heap_objects); return 2; }
  #endif
  return 0;
}
//#endif


