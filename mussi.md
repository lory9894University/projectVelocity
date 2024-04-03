File in arrivo sul gestionale. 
Il gestionale di suo genera degli eventi. ma non li ha tutti, quindi gli altri li calcoliamo noi.
il T&T legacy si occupa di calcolare questi eventi. Velocity è più reattivo.
Il nuovo tracking è identico, perchè sta ricreandosi gli oggetti come il vecchio tracking.
con calma si sta facendo il roll out di un sistema che si colleghi direttamente a velocity.
Ci sono più tms. il T&T li consulta tutti.

scritto in .net, servizio windows. Accede direttamente ai db e ha un suo db interno. gira su server virtuali windows di google.
Ha il suo modello dati, prende i da
il nuovo è sempre scritto in dotnet, ma è un servizio cloud (azure). il nuovo traking attinge da velocity.
il nuovo trakking è una azure function sempre in c#
