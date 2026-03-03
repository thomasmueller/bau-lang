// https://gist.github.com/karpathy/8627fe009c40f57531cb18360106ce95
// https://news.ycombinator.com/item?id=47202708
// https://karpathy.github.io/2026/02/12/microgpt/
package org.bau.stdlib.nn;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class MicroGPT {

    public static void main(String[] args) throws Exception {
        if (!Files.exists(Path.of("input.txt"))) {
            InputStream in = MicroGPT.class.getResourceAsStream("names.txt");
            Files.copy(in, Path.of("input.txt"));
            in.close();
        }
        List<String> docs = Files.readAllLines(Path.of("input.txt")).stream().map(String::trim)
                .filter(s -> !s.isEmpty()).collect(Collectors.toList());

        Collections.shuffle(docs, random);

        Set<Character> charset = new HashSet<>();
        for (String s : docs)
            for (char c : s.toCharArray())
                charset.add(c);

        List<Character> uchars = new ArrayList<>(charset);
        Collections.sort(uchars);

        int BOS = uchars.size();
        int vocab_size = uchars.size() + 1;

        // Initialize weights
        state.put("wte", matrix(vocab_size, n_embd, 0.08));
        state.put("wpe", matrix(block_size, n_embd, 0.08));
        state.put("lm_head", matrix(vocab_size, n_embd, 0.08));

        for (int i = 0; i < n_layer; i++) {
            state.put("layer" + i + ".attn_wq", matrix(n_embd, n_embd, 0.08));
            state.put("layer" + i + ".attn_wk", matrix(n_embd, n_embd, 0.08));
            state.put("layer" + i + ".attn_wv", matrix(n_embd, n_embd, 0.08));
            state.put("layer" + i + ".attn_wo", matrix(n_embd, n_embd, 0.08));
            state.put("layer" + i + ".mlp_fc1", matrix(4 * n_embd, n_embd, 0.08));
            state.put("layer" + i + ".mlp_fc2", matrix(n_embd, 4 * n_embd, 0.08));
        }

        for (Value[][] mat : state.values())
            for (Value[] row : mat)
                params.addAll(Arrays.asList(row));

        System.out.println("Params: " + params.size());
        // Adam buffers
        double learning_rate = 0.01;
        double beta1 = 0.85;
        double beta2 = 0.99;
        double eps = 1e-8;

        double[] m = new double[params.size()];
        double[] v = new double[params.size()];

        int num_steps = 1000;

        for (int step = 0; step < num_steps; step++) {

            String doc = docs.get(step % docs.size());

            List<Integer> tokens = new ArrayList<>();
            tokens.add(BOS);
            for (char c : doc.toCharArray())
                tokens.add(uchars.indexOf(c));
            tokens.add(BOS);

            int n = Math.min(block_size, tokens.size() - 1);

            List<List<List<Value>>> keys = new ArrayList<>();
            List<List<List<Value>>> values = new ArrayList<>();
            for (int i = 0; i < n_layer; i++) {
                keys.add(new ArrayList<>());
                values.add(new ArrayList<>());
            }

            List<Value> losses = new ArrayList<>();

            for (int pos = 0; pos < n; pos++) {
                int tokenId = tokens.get(pos);
                int targetId = tokens.get(pos + 1);

                List<Value> logits = gpt(tokenId, pos, keys, values);
                List<Value> probs = softmax(logits);

                Value loss_t = probs.get(targetId).log();
                loss_t = Value.mul(new Value(-1), loss_t);
                losses.add(loss_t);
            }

            Value loss = new Value(0);
            for (Value l : losses)
                loss = Value.add(loss, l);

            loss = Value.mul(loss, new Value(1.0 / n));

            // Backward
            loss.backward();

            // Adam update
            double lr_t = learning_rate * (1.0 - (double) step / num_steps);

            for (int i = 0; i < params.size(); i++) {
                Value p = params.get(i);

                m[i] = beta1 * m[i] + (1 - beta1) * p.grad;
                v[i] = beta2 * v[i] + (1 - beta2) * p.grad * p.grad;

                double m_hat = m[i] / (1 - Math.pow(beta1, step + 1));
                double v_hat = v[i] / (1 - Math.pow(beta2, step + 1));

                p.data -= lr_t * m_hat / (Math.sqrt(v_hat) + eps);
                p.grad = 0;
            }

            if ((step + 1) % 10 == 0)
                System.out.printf("step %4d / %4d | loss %.4f\r", step + 1, num_steps, loss.data);
        }

        System.out.println("\n--- inference ---");

        double temperature = 0.5;

        for (int sampleIdx = 0; sampleIdx < 20; sampleIdx++) {

            List<List<List<Value>>> keys = new ArrayList<>();
            List<List<List<Value>>> values = new ArrayList<>();
            for (int i = 0; i < n_layer; i++) {
                keys.add(new ArrayList<>());
                values.add(new ArrayList<>());
            }

            int tokenId = BOS;
            StringBuilder sample = new StringBuilder();

            for (int pos = 0; pos < block_size; pos++) {

                List<Value> logits = gpt(tokenId, pos, keys, values);

                List<Value> scaled = new ArrayList<>();
                for (Value l : logits)
                    scaled.add(Value.mul(l, new Value(1.0 / temperature)));

                List<Value> probs = softmax(scaled);

                double r = random.nextDouble();
                double cum = 0;
                int nextToken = 0;

                for (int i = 0; i < probs.size(); i++) {
                    cum += probs.get(i).data;
                    if (r <= cum) {
                        nextToken = i;
                        break;
                    }
                }

                if (nextToken == BOS)
                    break;

                sample.append(uchars.get(nextToken));
                tokenId = nextToken;
            }

            System.out.printf("sample %2d: %s\n", sampleIdx + 1, sample);
        }
    }

    static Random random = new Random(42);

    // -------------------- Autograd --------------------

    static class Value {
        double data;
        double grad = 0.0;
        List<Value> children;
        List<Double> localGrads;

        Value(double data) {
            this.data = data;
            this.children = new ArrayList<>();
            this.localGrads = new ArrayList<>();
        }

        Value(double data, List<Value> children, List<Double> localGrads) {
            this.data = data;
            this.children = children;
            this.localGrads = localGrads;
        }

        static Value add(Value a, Value b) {
            return new Value(a.data + b.data, Arrays.asList(a, b), Arrays.asList(1.0, 1.0));
        }

        static Value mul(Value a, Value b) {
            return new Value(a.data * b.data, Arrays.asList(a, b), Arrays.asList(b.data, a.data));
        }

        Value pow(double exp) {
            return new Value(Math.pow(this.data, exp), List.of(this), List.of(exp * Math.pow(this.data, exp - 1)));
        }

        Value log() {
            return new Value(Math.log(this.data), List.of(this), List.of(1.0 / this.data));
        }

        Value exp() {
            double e = Math.exp(this.data);
            return new Value(e, List.of(this), List.of(e));
        }

        Value relu() {
            return new Value(Math.max(0, this.data), List.of(this), List.of(this.data > 0 ? 1.0 : 0.0));
        }

        void backward() {
            List<Value> topo = new ArrayList<>();
            Set<Value> visited = new HashSet<>();

            buildTopo(this, visited, topo);

            this.grad = 1.0;
            Collections.reverse(topo);

            for (Value v : topo) {
                for (int i = 0; i < v.children.size(); i++) {
                    Value child = v.children.get(i);
                    double localGrad = v.localGrads.get(i);
                    child.grad += localGrad * v.grad;
                }
            }
        }

        static void buildTopo(Value v, Set<Value> visited, List<Value> topo) {
            if (!visited.contains(v)) {
                visited.add(v);
                for (Value child : v.children) {
                    buildTopo(child, visited, topo);
                }
                topo.add(v);
            }
        }
    }

    // -------------------- Model Config --------------------

    static int n_layer = 1;
    static int n_embd = 16;
    static int block_size = 16;
    static int n_head = 4;
    static int head_dim = n_embd / n_head;

    static Map<String, Value[][]> state = new HashMap<>();
    static List<Value> params = new ArrayList<>();

    static Value[][] matrix(int rows, int cols, double std) {
        Value[][] m = new Value[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                m[i][j] = new Value(random.nextGaussian() * std);
        return m;
    }

    // -------------------- Layers --------------------

    static List<Value> linear(List<Value> x, Value[][] w) {
        List<Value> out = new ArrayList<>();
        for (Value[] row : w) {
            Value sum = new Value(0);
            for (int i = 0; i < row.length; i++) {
                sum = Value.add(sum, Value.mul(row[i], x.get(i)));
            }
            out.add(sum);
        }
        return out;
    }

    static List<Value> softmax(List<Value> logits) {
        double max = logits.stream().mapToDouble(v -> v.data).max().getAsDouble();

        List<Value> exps = new ArrayList<>();
        for (Value v : logits)
            exps.add(Value.add(v, new Value(-max)).exp());

        Value total = new Value(0);
        for (Value e : exps)
            total = Value.add(total, e);

        List<Value> probs = new ArrayList<>();
        for (Value e : exps)
            probs.add(Value.mul(e, total.pow(-1)));

        return probs;
    }

    static List<Value> rmsnorm(List<Value> x) {
        Value ms = new Value(0);
        for (Value v : x)
            ms = Value.add(ms, Value.mul(v, v));

        ms = Value.mul(ms, new Value(1.0 / x.size()));
        Value scale = Value.add(ms, new Value(1e-5)).pow(-0.5);

        List<Value> out = new ArrayList<>();
        for (Value v : x)
            out.add(Value.mul(v, scale));
        return out;
    }

    // -------------------- GPT Forward --------------------

    static List<Value> gpt(int tokenId, int posId, List<List<List<Value>>> keys, List<List<List<Value>>> values) {

        List<Value> tokEmb = Arrays.asList(state.get("wte")[tokenId]);
        List<Value> posEmb = Arrays.asList(state.get("wpe")[posId]);

        List<Value> x = new ArrayList<>();
        for (int i = 0; i < n_embd; i++)
            x.add(Value.add(tokEmb.get(i), posEmb.get(i)));

        x = rmsnorm(x);

        for (int li = 0; li < n_layer; li++) {

            List<Value> xResidual = x;
            x = rmsnorm(x);

            List<Value> q = linear(x, state.get("layer" + li + ".attn_wq"));
            List<Value> k = linear(x, state.get("layer" + li + ".attn_wk"));
            List<Value> v = linear(x, state.get("layer" + li + ".attn_wv"));

            keys.get(li).add(k);
            values.get(li).add(v);

            List<Value> xAttn = new ArrayList<>();

            for (int h = 0; h < n_head; h++) {
                int hs = h * head_dim;

                List<Value> qh = q.subList(hs, hs + head_dim);

                List<Value> attnLogits = new ArrayList<>();
                for (int t = 0; t < keys.get(li).size(); t++) {
                    List<Value> kh = keys.get(li).get(t).subList(hs, hs + head_dim);
                    Value sum = new Value(0);
                    for (int j = 0; j < head_dim; j++)
                        sum = Value.add(sum, Value.mul(qh.get(j), kh.get(j)));
                    attnLogits.add(Value.mul(sum, new Value(1.0 / Math.sqrt(head_dim))));
                }

                List<Value> attnWeights = softmax(attnLogits);

                for (int j = 0; j < head_dim; j++) {
                    Value sum = new Value(0);
                    for (int t = 0; t < attnWeights.size(); t++) {
                        List<Value> vh = values.get(li).get(t).subList(hs, hs + head_dim);
                        sum = Value.add(sum, Value.mul(attnWeights.get(t), vh.get(j)));
                    }
                    xAttn.add(sum);
                }
            }

            x = linear(xAttn, state.get("layer" + li + ".attn_wo"));
            for (int i = 0; i < n_embd; i++)
                x.set(i, Value.add(x.get(i), xResidual.get(i)));
            xResidual = x;
            x = rmsnorm(x);
            x = linear(x, state.get("layer" + li + ".mlp_fc1"));
            for (int i = 0; i < x.size(); i++)
                x.set(i, x.get(i).relu());
            x = linear(x, state.get("layer" + li + ".mlp_fc2"));
            for (int i = 0; i < n_embd; i++)
                x.set(i, Value.add(x.get(i), xResidual.get(i)));
        }

        return linear(x, state.get("lm_head"));
    }
}