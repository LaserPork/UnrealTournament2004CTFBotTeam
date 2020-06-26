# UnrealTournament2004CTFBotTeam
Implementation of multi agent bot team that captures the flag in the game Unreal Tournament 2004 using Pogamut 3


Projekt obsahuje spustitelný JAR soubor pojmenovany isw-2020-vaverkajakub-jar-1.2.jar. Ten je spustitelny samostatne, tj. vsechny knihovni tridy, ktere pouziva jsou pribaleny v tomto JAR souboru. Soubor po spusteni rozbehne tym botu a pripoji je k serveru. Soubor bude spousten s nasledujicimi parametry:
1. parametr: 2020
2. parametr: 0 nebo 1, kde 0 je cerveny tym a 1 je modry
3. parametr: 4 až 7 je skill botu
4. parametr: 3 až 8 ... to je počet botů v týmu
5. parametr: localhost nebo xx.xx.xx.xx ... IP adresa serveru

Priklad spusteni:
java -jar isw-2020-vaverkajakub-jar-1.2.jar 2020 0 4 3 localhost
Jiny priklad spusteni:
java -jar isw-2020-vaverkajakub-jar-1.2.jar 2020 1 4 3 147.228.63.149
