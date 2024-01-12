# Prefazione, sitazione attuale
Velocity è *Event Driven*, lo scopo è il monitoraggio in near real time della logistica.
Al momento il sistema monolitico legacy gestisce tutto, il nuovo sistema (Velocity) è in fase di sviluppo. 
In pieno approcio Brownfield si stanno integrando i nuovi servizi con il vecchio sistema, in modo da poter effettuare il passaggio in modo graduale.
I clienti che chiedono funzionalità già implementate quindi sono sul nuovo sistema, i clienti che che hanno richiesto in passato queste funzionalità sono sul vecchio sistema.
Al momento un solo cliente è sul nuovo sistema (lo chiamiamo cliente Pilota), se la sperimetazione va bene si potrà passare gradualmente tutti i clienti sul nuovo sistema.
(TUTTO QUESTO DISCORSO VA SCRITTO NELLA TESI, il discorso del bronwfield e del cliente pilota e di transizione graduale)
Entro fine gennaio fare un meeting con tutor + realtore in modo da capire come includere anche il discorso del cliente pilota e del brownfield nella tesi. 

## Obbiettivi:
- liberarsi di Spring Batch e passare ad Apache Flink.
- Cambiare orchestratore, da Talend a quello di Google oppure a Mulesoft, dato che l'altro è morto. (cosa non sicura, ancora da discutere)
Questo si adatta bene anche al passo 1 dato che il nuovo orchestratore gestisce nativamente i il sistema di messaggistica di Apache.

Il mio lavoro sarà una sperimentazione, per vedere se è possibile effettuare questi due passaggi e se effettivamente forniscono un miglioramento delle prestazioni.
Successivamente si potrà decidere se effettuare il passaggio o meno ed in tal caso mi occupero di effettuare il passaggio.

# Step 0 (studiare il codice)
## MicroBatch
A grandi linee questo microservizio "ricostruisce" una entità a partire da tutti gli eventi che la riguardano, poi la scrive su Fast Storage.
Gli eventi non sono presi dal topic Kafka, bensì dal Fast Storage, in questo caso un db SQL (microsoft sqlserver).

Dopo la "ricostruzione" l'oggetto viene riscritto nel Fast Storage, credo eliminando gli eventi che lo riguardavano e che non sono più necessari (ma anche no, potrebbero servire ad altri. nel dubbio ==TODO chiedi a Michele==).

La ricostruzione avviene in 3 fasi:
1. **Reader**: Durante la fase di Reader vengono recuperati dal Fast Storage tutti gli Eventi di Dominio che sono stati assegnati dal [partizionatore](#partizionatore) a quello specifico chunk.
Il passo fondamentale è il metodo *findByDomainEventCollectionIdIn()* che ritorna tutti gli eventi di dominio (DomainEventEntity) che hanno un determinato DomainEventCollectionId. 
(in realtà non quelli che hanno un determinato DomainEventCollectionId, ma quelli che hanno un DomainEventCollectionId che è presente nella lista di DomainEventCollectionId passata come parametro, non so perchè una lista al posto di un singolo valore ... già che li ho tanto vale processarli? )
*findByDomainEventCollectionIdIn()* è un metodo di JpaRepository, il classico generato automagicamente da Spring Boot.
Coopilot dice questo : 
```
This method is a part of Spring Data JPA's query creation mechanism. The method name findByDomainEventCollectionIdIn is parsed by Spring Data JPA to create a query. The findBy part is a reserved keyword in Spring Data, indicating a select query. DomainEventCollectionId is the property of the entity on which the select operation will be performed. The In keyword indicates that the method will return a list of DomainEventEntity objects where the DomainEventCollectionId is in the provided list of IDs (domainEventCollectionIdList)
```
2. **Processor**: Fase in cui si trasformano gli Eventi di Dominio recuperati durante la fase di Reader in una serie di record pronti alla scrittura, ovvero in una serie di oggetti di tipo Entity. 
Oggetti Java proprio, prima erano Eventi, ora sono Oggetti.
==TODO C'è tutto un discorso di mappatura su tabelle che non ho capito pag 29 di MicroBatch, chiedere a Michele==.
Lo si fa tramite *MapStruct*, che è una libreria che permette di mappare oggetti di tipo diverso, in questo caso da Evento a Oggetto.

3. **Writer**: Fase finale di scrittura sul Fast Storage.
É una scrittura transazionale, quindi credo ACID.
La seconda cosa importante e il metodo writeItemToDB().
Questo metodo è molto importante perché definisce per ciascuna tabella le logiche secondo le quali alcuni dati devo essere inseriti ed altri solo aggiornati, ad esempio verificando il tipo di operazione contenuta nell’Evento di Dominio e il valore di Revision dell’evento stesso.

### Partizionatore
Perchè a noi la sequenzialità fa schifo.
Il partizionatore è un componente che si occupa di dividere gli eventi di dominio in chunk, in modo da poterli processare in parallelo.
Quindi le 3 fasi riportate sopra vengono eseguite in parallelo su più chunk, il numero di partizioni è configurabile, ed è stato sviluppato un partizionatore custom che raggruppa le chiavi di dominio uguali nelle stesse partizioni.

La prima operazione svolta da ciascun Job è quella di chiamare una Stored Procedure sviluppata sul Fast Storage che svolge la funzione di sincronizzatore e che ha due compiti principali:
1. recuperare dalle tabelle Flow Manager i record con Status = “Ready” e portarli in Status “In Progress”; 
2. “pacchettizzare” questi record assegnando a ciascuno un valore di Group_Execute_ID.

## Event Engine
A grandi linee questo microservizio si occupa di "calcolare" degli eventi di business partendo dal Fast Storage.
Quali eventi? “spedizione partita”, “ritiro fallito”, “tempo di arrivo stimato” ....
Come? andando a leggere gli eventi di dominio (generati dal SGA)

==TODO cosa sia SGA al momento mi sfugge, sarà il TMS? magari chiedere a Michele==

Nel documento c'e scritto "Rispetto al caso Micro Batch, la Stored Procedure ritorna all’applicazione solo un record per ogni chiave di dominio". quindi ad ogni chunk corrisponde una e solo una chiave di dominio.
Solite tre fasi: Reader, Processor, Writer.
1. **Reader**: Partendo dalla chiave di dominio si recuperano tutti gli eventi di dominio che la riguardano. Importante la classe *ItemReader* che genera l'oggetto vero e proprio partendo da dei Repository (utile anche per vedere quali tipo di info vengono caricate)
L'output di questa fase è un oggetto del tipo *TransportWorkloadEntity* che contiene tutti gli eventi di dominio che riguardano la chiave di dominio.
2. **Processor**: La fase di Processor è responsabile della creazione vera e propria degli eventi di business calcolati dall’applicativo. gestita tutta da *it.batch.jobs.items.processor.EventEngineProcessor* in particolare il metodo **process**.
Calcola una serie di metriche molto specifiche che non sto a riportare, ma che sono tutte scritte nel documento. Vengono tutte effettuate su un oggetto del tipo *TransportWorkloadEntity* che mi sembra di aver capito essere un "ordine di trasporto". In sintesi però il processo è questo:
- calcolo eventi di business SGA, una serie di sigle che non capisco, specifiche del settore logistico.
- milestonesTOLeg, anche qui, non so cosa sia TOLeg, ma e qualcosa di legato a come il pacco viene spedito (direct transport, shuttle, delivery, ...)
- stato dell'ordine di trasporto, associamo alla *TransportWorkloadEntity* uno stato ( GIA, CON, COP, ICO, IVI) cosa vogliono dire non si sa.
- calcolo eventi corporate. altra roba sconosciuta specificata in questo documento: *Order_LifeCycle_Status-Events_OM_rev05.wlsx*
- Calcolo delle handling units da associare agli eventi corporate
3. **Writer**: Abbiamo il nostro *TransportWorkloadEntity* con tutti i dati elaborati e li dobbiamo riscrivere sul Fast Storage.
Può però verificarsi il caso in cui l’informazione salvata su Fast Storage potrebbe essere momentaneamente incompleta, ad esempio perché è stato elaborato un evento di giacenza prima dell’effettiva creazione dell’ordine di trasporto a cui quella giacenza è collegata.
Per tale ragione gli eventi elaborati hanno un attributo che può essere i **COMPLETE** o **ONLY_SIGNALING**.
Nel caso di **COMPLETE** l’evento viene scritto su Fast Storage (e su di un topic kafka ***Signaling Topic***), nel caso di **ONLY_SIGNALING** l’evento viene scritto solo sul Topic Kafka (in realtà anche su una tabela dedicata di Fast Storage, Transactional.SignalingTopics).
Attento a non confondere il l'evento con la *TrasnportWorkloadEntity*, se l'evento non è completo effettivamente non vengono scritte una serie di cose sul Fast Storage (tabelle TransportOrdersEvents, TOLeg, Transactional.TransportOrder e TransportOrdersEventsCorporate), ma c'è comunque una operazione di scrittura del'oggetto *TransportWorkloadEntity*  (EventEngineWriter.java righe 129-137).

## Kafka Streams
Documento generico di tutti i *Kafka Streams* sviluppati, prendo come esempio pratico *Velocity-Spedizioni*.

Prima nota interessante: Non viene usato ***Debezium*** duro e puro, bensì lo usiamo sotto forma di *Kafka Connector* (il plugin in questione è [debezium-connector-sqlserver](https://www.confluent.io/hub/debezium/debezium-connector-sqlserver)). 
Seconda nota, c'è uno **Schema Registry**, questa infrastruttura è uscita direttamente dal corso di Confluent.
Gli schemi sono *Avro*, e si posso trovare in *src/main/resources/avro/*

### Ricezione di eventi
Il nostro bravo Debezium si occupa di rilevare i cambiamenti sul DB e li pubblica su diversi topic kafka, uno per tabella. ( se non ricordo male c'è una tabella per ogni **Dominio**).
tramite il Kafka Streams si va a leggere questi topic, in particolare la classe *it.quantyca.kafka.streams.processors.keyvaluemappers.KeyValueMapper* fa la seguente operazione:
Genera una chiave ``` <istanza db sorgente> + ">" + <id_transaction> + "-" + <chiave logica testata> ```, crea un nuovo *TransactionEvent* con tale chiave e con contenuto il contenuto del messaggio CDC di Debezium, publica tale oggetto su un topic kafka dedicato ***single***.

A seconda dello stream che stiamo sviluppando possiamo guardare solo un DB piuttosto che un altro, si specifica il db nella proprietà *database.list* di *application.properties*

### Aggregazione di eventi
Ottenuto lo stream di eventi ben formato possiamo avere 3 tipi di aggregazione: (classe *StreamAggregator*)
1. eventLocalStream 
2. endTransactionGlobalTable. Filtro gli eventi di *endTransaction* ==TODO cosa diavolo è un evento di END_TRANSACTION? probabilmente roba di Debezium, ma c'è uno State-Store ed un aggregatore dedicato *endTransactionGlobalTable*==

## Transaction Processing
Vogliamo passare da un *TransactionEvent* ad un *Domain Event*. Classe di riferimento *DomainEventPublisher*.

# Step 1 (Flink Spike)
## Obbiettivo
Sperimentare Apache Flink per capire se è possibile effettuare il passaggio da Spring Batch a Flink.

Ciao, ti invio un altro messaggio di riepilogo, leggilo quando hai tempo, tanto fino a Martedì sono impegnato con i corsi di formazione.

Ho guardato tutto quello che mi avevi detto (Event-engine, Micro-batch, Kafka Streams, Micro-batch-FTL). Tutto abbastanza chiaro, ho un paio di domande che vorrei farti.
Ci ho messo un po' perchè per capire tutto ho dovuto leggere anche un po' di documentazione su Kafka e su Stream, Connector, etc... (ho guardato un breve corso). Oggi pomeriggio volevo iniziare la stesura della tesi (dato che ho fresca in mente l'architettura), ma se preferisci che ci sentiamo ci sono. Altrimenti, senza fretta, la prossima settimana.