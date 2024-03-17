    SingleOutputStreamOperator<Tuple3<[...]>> noEmptySignalingEventStream =
    tradingPartnerPresentStream.filter(x -> !x.getSignalingEvent().isEmpty())
    //mappa un flusso di tuple (SignalingTopicRecord, SignalingEvent_record) a partire da un flusso di SignalingTopicRecord
    .flatMap(new FlatMapFunction<SignalingTopicRecord, Tuple2<SignalingTopicRecord, SignalingEvent_record>>() {
        @Override
        public void flatMap(SignalingTopicRecord value, Collector<Tuple2<SignalingTopicRecord, SignalingEvent_record>> out) throws Exception {
        for (SignalingEvent_record signalingEvent_record : value.getSignalingEvent()) {
            out.collect(new Tuple2<>(value, signalingEvent_record));
            }
        }

        })
    .filter(x -> x.f1.getOperationType() != "d")
    .connect(tradingPartnerBroadcastRules)
    .process(new TradingPartnerRuleEvaluator());