DATA=data/about.html data/icon.gif data/intro.html data/pref.html data/ref.html pref/TwidlitUnprintable.txt pref/TwidlitKeyNames.txt pref/TwidlitKeyValues.txt pref/TwidlitPersist.properties pref/TwidlitPreferences.txt 
JAR_DATA=${DATA}
IO=pkp/io/Io.class pkp/io/LineReader.class pkp/io/SpacedPairReader.class
STRING=pkp/string/StringSource.class pkp/string/StringInt.class pkp/string/StringsInts.class pkp/string/StringsIntsBuilder.class 
LOOKUP=pkp/lookup/Lookup.class pkp/lookup/LookupSet.class pkp/lookup/LookupImplementation.class pkp/lookup/LookupBuilder.class pkp/lookup/SharedIndex.class
UI=pkp/ui/ControlDialog.class pkp/ui/ControlWindow.class pkp/ui/ExtensionFileFilter.class pkp/ui/HtmlWindow.class pkp/ui/IntegerTextField.class pkp/ui/LabelComponentBox.class pkp/ui/PersistentDialog.class pkp/ui/PersistentFrame.class pkp/ui/ProgressWindow.class pkp/ui/SaveTextWindow.class pkp/ui/SliderBuilder.class pkp/ui/TextWindow.class
UTIL=pkp/util/Log.class pkp/util/Persist.class pkp/util/Persistent.class pkp/util/PersistentProperties.class pkp/util/Pref.class 
CHARS=pkp/chars/CharCounts.class pkp/chars/Counts.class pkp/chars/NGram.class pkp/chars/NGrams.class
TWIDDLE=pkp/twiddle/Assignment.class pkp/twiddle/Chord.class pkp/twiddle/KeyMap.class pkp/twiddle/KeyPress.class pkp/twiddle/KeyPressList.class pkp/twiddle/Modifiers.class pkp/twiddle/ThumbKeys.class pkp/twiddle/Twiddle.class 
TWIDDLER=pkp/twiddler/Cfg.class pkp/twiddler/Settings.class pkp/twiddler/SettingsWindow.class 
TWIDLIT=pkp/twidlit/CountsRangeSetter.class pkp/twidlit/Twidlit.class pkp/twidlit/TwidlitMenu.class
CLASSES= ${TEST} ${IO} ${STRING} ${LOOKUP} ${UI} ${UTIL} ${CHARS} ${TWIDDLE} ${TWIDDLER} ${TWIDLIT}
CLEAN=rm *.class *~ *.bak tmp

all: classes
jar: Twidlit0.jar
io: ${IO}
string: ${STRING}
lookup: ${LOOKUP}
ui: ${UI}
util: ${UTIL}
chars: ${CHARS}
twiddle: ${TWIDDLE}
twiddler: ${TWIDDLER}
twidlit: ${TWIDLIT}
classes: io string lookup ui util chars twiddle twiddler twidlit
clean:
	cd pkp/io; ${CLEAN}; cd -
	cd pkp/string; ${CLEAN}; cd -
	cd pkp/lookup; ${CLEAN}; cd -
	cd pkp/ui; ${CLEAN}; cd -
	cd pkp/util; ${CLEAN}; cd -
	cd pkp/chars; ${CLEAN}; cd -
	cd pkp/twiddle; ${CLEAN}; cd -
	cd pkp/twiddler; ${CLEAN}; cd -
	cd pkp/twidlit; ${CLEAN}; cd -
	${CLEAN} classlist.txt TwidlitLog.txt TwidlitPersist.properties
   
%.class: %.java
	javac $<

# list of the classes to jar, escaping the $s
classlist.txt: ${CLASSES}
	find . -iname \*.class | sed 's/\$$/\\$$/' > $@

Twidlit0.jar: Manifest.txt ${JAR_DATA} classlist.txt
	jar cfm $@ Manifest.txt ${JAR_DATA} $(shell cat classlist.txt)
	cp Twidlit0.jar ../Program\ Files/Twidlit

