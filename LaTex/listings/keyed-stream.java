...

dataStream.keyBy(value -> value.f0)
.reduce((accumulator, value2) -> {
    accumulator.f1 += value2.f1;
    return accumulator;
    });

...