final MapStateDescriptor<String, TradingPartnerEventLookup> tradingPartnerBroadcastDescriptor =
    new MapStateDescriptor<>("tradingPartnerBroadcastDescriptor", 
                            Types.STRING,
                            Types.POJO(TradingPartnerEventLookup.class));

DataStream<List<tradingPartnerEventLookup>>tradingPartnerEventLookupStream = 
    sqlSource.getTradingPartnerEventLookupStream();
BroadcastStream<List<TradingPartnerEventLookup>> tradingPartnerBroadcastRules = 
    tradingPartnerEventLookupStream.broadcast(tradingPartnerBroadcastDescriptor);