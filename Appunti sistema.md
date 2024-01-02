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

# Step 1 (Flink Spike)
## Obbiettivo
Sperimentare Apache Flink per capire se è possibile effettuare il passaggio da Spring Batch a Flink.