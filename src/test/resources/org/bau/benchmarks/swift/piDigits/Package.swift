// swift-tools-version: 6.0

import PackageDescription

let package = Package(
    name: "piDigits",
    dependencies: [
        .package(url: "https://github.com/attaswift/BigInt.git", from: "5.4.0")
    ],    
    targets: [
        .executableTarget(
            name: "piDigits",
            dependencies: [
                .product(name: "BigInt", package: "BigInt")
            ]
        )
    ]
)
