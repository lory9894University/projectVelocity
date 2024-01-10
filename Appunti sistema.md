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
==TODO C'è tutto un discorso di mappatura su tabelle che non ho capito, chiedere a Michele==.
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

# Step 1 (Flink Spike)
## Obbiettivo
Sperimentare Apache Flink per capire se è possibile effettuare il passaggio da Spring Batch a Flink.