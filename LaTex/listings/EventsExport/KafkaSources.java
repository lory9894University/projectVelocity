public KafkaSources() {
  Properties properties = new Properties();
    try {
      Reader reader = new FileReader(new File("kafka.properties"));
      properties.load(reader);
      reader.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      exit(1);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    //Creazione del KafkaSource per il topic "SignalingTopics"
    EventSource = KafkaSource.<SignalingTopicRecord>builder()
      .setProperties(properties)
      //Properties per la connessione a Kafka (server, parametri SASL, etc..)
      .setTopics("SignalingTopics")
      .setStartingOffsets(OffsetsInitializer.earliest())
      .setValueOnlyDeserializer(ConfluentRegistryAvroDeserializationSchema.
      forSpecific(SignalingTopicRecord.class, 
        properties.getProperty("schema.registry.url"),
        properties))
      //deserializzo i messaggi in formato Avro
      //specificando il tipo di record che mi aspetto (SignalingTopicRecord)
      .build();
  }