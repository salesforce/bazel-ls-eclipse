load("@rules_java//java:defs.bzl", "java_library")

java_library(
    name = "module4",
    srcs = glob(["java/src/**/*.java"]),
    visibility = ["//visibility:public"],
    deps = [
        "//module3",
    ],
)
