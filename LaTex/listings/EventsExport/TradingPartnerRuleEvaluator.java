public class TradingPartnerRuleEvaluator
        extends BroadcastProcessFunction<
        Tuple2<SignalingTopicRecord,SignalingEvent_record>,
        List<TradingPartnerEventLookup>,
        Tuple3<SignalingTopicRecord, SignalingEvent_record, TradingPartnerEventLookup>> {
    
    private MapStateDescriptor<String, TradingPartnerEventLookup> patternDescriptor;

    [...]

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        patternDescriptor = new MapStateDescriptor<>
            ("tradingPartnerBroadcastDescriptor", Types.STRING, Types.POJO(TradingPartnerEventLookup.class));
    }

    @Override
    public void processElement(
                Tuple2<SignalingTopicRecord, SignalingEvent_record> value,
                ReadOnlyContext ctx,
                Collector<Tuple3<[...]>> out) throws Exception {
        String key = value.f0.getTradingPartner().toString() + value.f0.getBusinessObjectType().toString() + value.f1.getEventCode().toString();
        //recupera il Broadcast State
        TradingPartnerEventLookup eventLookup = ctx.getBroadcastState(this.patternDescriptor)
                .get(key); //sfrutto la chiave per recuperare il record corrispondente dal broadcast state
        if (eventLookup != null) {
            if (Objects.equals(eventLookup.getActive(), "Y")) {
                out.collect(new Tuple3<[...]>(value.f0, value.f1, eventLookup));
        } else {
                log.info("-Evento non configurato. EventLookup=NULL." + " TradingPartner=" + value.f0.getTradingPartner()
                        + ". EventCode=" + value.f1.getEventCode()
                        + " SignalingID=" + value.f0.getSignalingID() + "\n" + value.f0.toString()) ;
        }
    }

    @Override
    public void processBroadcastElement(
                List<TradingPartnerEventLookup> value,
                Context ctx,
                Collector<Tuple3<SignalingTopicRecord, SignalingEvent_record, TradingPartnerEventLookup>> out) throws Exception {

        BroadcastState<String, TradingPartnerEventLookup> tradingPartnerRules = ctx.getBroadcastState(patternDescriptor);
        for (TradingPartnerEventLookup tradingPartnerEventLookup : value) {
            String key = tradingPartnerEventLookup.getTradingPartner() + tradingPartnerEventLookup.getBusinessObjectType() + tradingPartnerEventLookup.getErpEventCode();
            tradingPartnerRules.put(key, tradingPartnerEventLookup);
        }
    }
}