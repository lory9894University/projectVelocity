...

dataStream.keyBy(value -> value.f0)
.reduce((value1, value2) -> {
    value1.f1 += value2.f1;
    return value1;
    });

...