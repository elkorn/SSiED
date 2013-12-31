SSiED
=====

Tworzenie operatorów - tutorial
--------------------------------------------
W folderze <a href="https://github.com/elkorn/SSiED/tree/master/project">project</a> znajduj¹ siê projekty dla eclipse:

* RapidMiner\_Extension\_Template_Unuk
* Rapidminer_Unuk

Do skompilowania rozszerzenia dla rapidminera potrzebne s¹ obydwa projekty.

Nowe operatory definiowane s¹ w projekcie ``RapidMiner_Extension_Template_Unuk``. Przepis jak to zrobiæ:

1.  W katalogu ``src/com/rapidminer/`` definiujemy now¹ klasê dla operatora.

2.  Nowa klasa powinna rozszerzaæ klasê ``Operator`` i nadpisaæ nastêpuj¹ce metody:

 ``doWork`` - funkcje realizowane w trakcie przetwarzania danych

 ``getParameterTypes`` - parametry operatora

 Wejœcia i wyjœcia operatora definiowane s¹ jako pola klasy:

        private final InputPort exampleSetInput = getInputPorts().createPort("nazwa wejscia");
        private final OutputPort exampleSetOutput = getOutputPorts().createPort("nazwa wyjscia");

 Konstruktor ma postaæ:

        public NowyOperator(OperatorDescription description){
	          super(description);
        }

 W ciele konstruktora typowe jest nadawanie warunków pocz¹tkowych dla wejœæ/wyjœæ, ale tylko jak chcemy mieæ warninga w rapidminerze gdy coœ nie jest pod³¹czone.

 Przyk³adowe odbieranie danych z portów/ wysy³anie na port:

        ExampleSet inputExampleSet = exampleSetInput.getData(ExampleSet.class);
        exampleSetOutput.deliver(result); 

 Mo¿na siê posi³kowaæ istniej¹cym operatorem.
3.  Po zdefiniowaniu klasy nale¿y przejœæ do katalogu ``resources/com/rapidminer/resources/`` i w pliku ``BalancingOperators.xml`` dodaæ informacje o swoim operatorze:

        <group key="">
          <group key="data_transformation">
	        <group key="data_balancing">
	          <operator>
	            <key>unikalna_nazwa</key>
	            <class>com.rapidminer.NowyOperator</class>
	          </operator>
	        </group>
          </group>
        </group>

 Znaczniki ``<group key="nazwa">`` informuj¹ w jakiej kategorii zostanie umieszczony nowy operator (panel ``Operators`` w rapidminerze).

4.  Nazwa nadana w tagach ``<key></key>`` mo¿e zostaæ u¿yta do t³umaczenia nazwy operatora w pliku ``OperatorsDocTemplate.xml`` w katalogu``resources/com/rapidminer/resources/i18n/``. 


        <operator>
		  <key>unikalny_nazwa</key>
		    <name>Nazwa widoczna w rapidmierze</name>
		    <synopsis>Krótki opis</synopsis>
		    <help>D³ugi opis</help>
	    </operator>


5.  Budujemy projekt Ant'em poprzez plik ``build.xml``  (PPM -> Run as -> Ant build) w g³ównym katalogu.


 Zbudowane rozszerzenie znajdzie siê w projekcie ``Rapidminer_Unuk`` w folderze ``lib/plugins``. Wystarczy uruchomiæ ``lib/rapidminer.jar`` i wszystko powinno dzia³aæ. Rozszerzenie mo¿na te¿ skopiowaæ do analogicznego folderu w rapidminerze 6 i te¿ zadzia³a.
