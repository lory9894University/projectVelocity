# bug che riguardano anche spring.
## Logic determination event

file `it.quantyca.logic.utils.ExportEventsLogic.java` line 95-100

```java
        //QUERY 3
        LogicDeterminationEvent logicDeterminationEvent = logicDeterminationEventCache
                .getLogicDeterminationEventByIdEventCustomer(eventLookup.getIdEventCustomer());

        if (logicDeterminationEvent == null)
            logicDeterminationEvent = new LogicDeterminationEvent();
```
Secondo me dovrebbe segnalare un errore e scartare il record, qui invece viene creato un LogicDeterminationEvent vuoto.
Essendo vuoto in elaborazioni future potrebbe dare problemi (non mi è mai capitato che però ci sia un customer che è in "TradingPartnerEventLookup" ma non in "LogicDeterminationEvent").

## Query conditions

file `it.quantyca.logic.utils..java` line 119-134

```java
if(eventLookup.getQueryCondition() != null) {
        KeyT2BaseMittentiDestinatari key = filterRulesLogic.conditionSpBaseMittentiDestinatariExtracted(
                transportOrder, eventLookup.getQueryCondition(), transportOrdersEvents);
                if(key != null) {
                ................
                }
}
```
`eventLookup.getQueryCondition()` ha ritornato null in tutti i test che ho fatto, semplicemente non mi arrivano mai input da trading partner che hanno query condition impostate. Quindi non ho avuto modo di vedere se il resto del codice funziona correttamente.

## salvataggio su db prima dell'invio
file `it.quantyca.logic.utils.ExportEventsLogic.java` line 673

l'evento TransportOrdersExportEvents viene creato e salvato su db indipendentemente dal fatto che venga inviato o meno (su sterling o salvato su db nel caso di toyotamill).
Magari è corretto così, ma se dovesse esserci un errore sull'invio dell'evento questo non verrebbe notato e l'evento verrebbe comunque salvato su db.
Certo ci sono sempre i log, quindi è probabile mi stia preoccupando per niente. 

## Inconsistenze nel Database
Questi sono bug che riguardano la comunicazione con il database.
voi non avete mai avuto problemi con questi bug perchè non lavorate direttamente con Hybernate, ma con Spring che si occupa di tradurre le query.
E' probabile che Spring gestisca automaticamente la maggior parte di questi problemi, non presentando mai errori.
### inconsistenza tra db e codice 
nel file `it.quantyca.model.faststorage.entities.consumer.TransportOrdersExportEvents.java` line 40
EventDate è di tipo *String*, mentre nel db è di tipo *datetimeoffset*.
Hybernate non fa automaticamente il mapping tra i due tipi e mi da un errore.

Nel mio codice ho risolto il problema cambiando il tipo di EventDate da *String* a *OffsetDateTime* ed effettuando un parsing.
### inconsistenza tra tabelle
Ci sono due tabelle che pur indicando la stessa entità hanno un "tipo" diverso.
TransportOrdersInvPartyLinks ha un campo invPartyCode di tipo *nvarchar*, mentre SpBaseMittentiDestinatari ha un campo codice di tipo *int*.
chiamando la funzione findCustomDataFromSpBaseMittentiDestinatari nel file `it.quantyca.model.faststorage.repositories.transactional.TransportOrdersInvPartyLinksRepository.java` Hybernate mi da il seguente errore:
```
Cannot compare left expression of type 'java.lang.String' with right expression of type 'java.lang.Integer'
```
voi non avete questo problema perchè Spring si occupa di fare il cast automaticamente.
SpBaseMittentiDestinatari.codice è un Integer, mentre TransportOrdersInvPartyLinks.invPartyCode è un String
Ho risolto con un casting (nemmeno sapevo esistessero i casting in SQL....) ma qui c'è un errore nel modello del DB



# bug che riguardano solamente il mio codice.
## Leg references

file `it.quantyca.logic.utils.ExportEventsLogic.java` line 555-565

```java
for (LegReference legReference : legReferences) {
            TOEvent.TransportOrder.Event.TripReference.LegReferences.LegReference toEventLegReferenceObject = new TOEvent.TransportOrder.Event.TripReference.LegReferences.LegReference();
            for (Field field : fields) {
                Method legReferenceMethod = null;
                legReferenceMethod = LegReference.class.getDeclaredMethod("get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1));
                Object legReferenceField = legReferenceMethod.invoke(legReference);
                ConfigurablePropertyAccessor propertyAccessor = PropertyAccessorFactory.forBeanPropertyAccess(toEventLegReferenceObject);
                propertyAccessor.setPropertyValue(field.getName(), legReferenceField);
            }
            toEventLegReferencesForXml.add(toEventLegReferenceObject);
        }
```

A volte mi capita il seguente errore:
`object is not an instance of declaring class`
Credo che il problema non si sia mai presentato nel vostro codice perchè richiamate il metodo tramite ConfigurablePropertyAccessor, mentre io non avendo Spring sono costretto ad usare la reflection.
Probabilmente (non ne sono sicuro) Spring fa un controllo in più per evitare che vengano passati oggetti che non sono istanze della classe dichiarata, cosa che io non faccio.
Di base questo errore è un casino perchè non sono più riuscito a riprodurlo, ci sono degli eventi sul SignalingTopic che lo causano ma non sono riuscito a capire quali.

## findCustomSalesOrders return empty

Nel mio codice, la funzione findCustomSalesOrders nel file `it.quantyca.model.faststorage.repositories.transactional.CustomerSalesOrderRepository.java` line 39 ritorna sempre una lista vuota. non so se è colpa dei dati di input o della query, la query mi sembra sostanzialmente identica a quella scritta nel codice originale (quello con Spring).

Di nuovo, potrebbe semplicemente essere che non ho mai avuto input che soddisfano la query, ma mi sembra strano.

