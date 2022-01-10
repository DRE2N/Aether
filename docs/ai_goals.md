## avoid_target
_Sorgt dafür das ein Mob vor einem anderen wegläuft (Angst hat)_

``<prio>;avoid_target;<walk speed>;<sprint speed>;<Distance>;<target>``

* **Walk speed:** Normale Geschwindigkeit des Mobs
* **Sprint speed:** Geschwindigkeit, mit der Mob flieht wenn er ein Target entdeckt
* **Distance:** Entfernung, aber der Mob flieht.
* **Target:** Mob-Typ, vor dem geflohen wird. Hier zählt der Base-Typ (Mob auf dem Server), _nicht _der Display-Typ (Was der Spieler sieht)

**Beispiel:**
`` - 0;avoid_target;1.0;2.0;20;pig`` - Dieser Mob wird mit doppelter Geschwindigkeit (``2.0``) vor Schweinen (``pig``) fliehen, sobald diese näher als ``20`` Blöcke sind.

## avoid_water
_Wie ``random_stroll``, nur das dabei nie ins Wasser gegangen wird. Der Mob wird auch Wasser schnellstmöglich verlassen_

``<prio>;avoid_water;<speed>;<chance>``
* **Speed:** Laufgeschwindigkeit
* **Chance:** Wie häufig eine neue, zufällige Laufposition gesucht wird. Kleine Werte (0.01) sind ausreichend.

**Beispiel:** ``- 0;avoid_water;1.0;0.02``

## door_interact
_Erlaubt dem Mob, Türen zu öffnen. Mobs sind ordentlich, sie können also die Tür hinter sich auch wieder schließen._

``<prio>;door_interact;<close door>``
* **Close door:** true/false - Ob die Tür wieder geschlossen werden soll.

## flee_sun
_Der Mob wird versuchen, sich vor Sonnenlicht zu verstecken. Achtung: Funktioniert (wie in Vanilla) nur, wenn der Mob kein Item auf dem Kopf trägt._

``<prio>;flee_sun;<speed>``
* **Speed:** Geschwindigkeit mit der der Mob sich aus der Sonne bewegt.

## float
_Sorgt dafür, das der Mob im Wasser versucht an die Oberfläche zu schwimmen._

``<prio>;float``

## leap_at
_Der Mob springt mit erhöhter Geschwindigkeit auf das aktuelle Target zu. Dieses Goal nutzen beispielsweise normale Spinnen._

``<prio>;leap_at;<velocity>``
* **Velocity:** Geschwindigkeit, mit der sich der Mob im Sprung bewegt. Spinnen nutzen ``0.4``

## look_at_players
_Der Mob versucht, den nächsten Spieler anzuschauen._

``<prio>;look_at_players;<distance>;<chance>``
* **Distance:** Maximale Entfernung in der Spieler angeschaut werden.
* **Chance:** Wahrscheinlichkeit. Beeinflusst, wie oft der Mob versucht einen Spieler anzusehen.

## melee_attack
_Nahkampfangriff._

 ``<prio>;melee_attack;<speed>;<follow target>``
* **Speed:** Geschwindigkeit, mit der sich der Mob auf das Ziel zubewegt. _Nicht_ die Angriffsgeschwindigkeit - dafür Attribute nutzen.
* **Follow target:** true/false - Ob der Mob versuchen soll das Ziel zu erreichen auch wenn er es nicht sehen kann.
# panic
_Der Mob rennt wild im Kreis. Das ist alles. Wahrscheinlich nur sinnvoll wenn von einer Quest hinzugefügt._

``<prio>;panic;<speed>``
* **Speed:** Geschwindigkeit, mit der im Kreis gerannt wird.

## random_look_around
_Der Mob schaut in zufällige Richtungen._

``<prio>;random_look_around``

## random_stroll
_Der Mob läuft zufällig durch die Gegend._

``<prio>;random_stroll;<speed>;<chance>``
* **Speed:** Laufgeschwindigkeit
* **Chance:** Wie häufig eine neue, zufällige Laufposition gesucht wird. Kleine Werte (0.01) sind ausreichend.

## random_swim
_Der Mob schwimmt zufällig durch die Gegend. Wie random_stroll, nur im Wasser._

``<prio>;random_swim;<speed>;<chance>``
* **Speed:** Schwimmgeschwindigkeit
* **Chance:** Wie häufig eine neue, zufällige Position gesucht wird. Kleine Werte (0.01) sind ausreichend.

## ranged_bow_attack
_Der Mob versucht mit einem Bogen anzugreifen. Führt auch zu Strafing. Setzt natürlich eine Fernkampfwaffe voraus._

``<prio>;ranged_bow_attack;<speed>;<interval>;<radius>``
* **Speed:** Laufgeschwindigkeit beim Angreifen.
* **Interval:** Angriffsgeschwindigkeit.
* **Radius:** Entfernung, ab der Angegriffen wird.

## ranged_crossbow_attack
_Der Mob versucht mit einer Armbrust anzugreifen. Setzt natürlich eine Armbrust voraus. Im Gegensatz zu ranged_bow_attack ohne Strafing._

``<prio>;ranged_bow_attack;<<interval>;<radius>``
* **Interval:** Angriffsgeschwindigkeit.
* **Radius:** Entfernung, ab der Angegriffen wird.
