JAR_DATA=data/about.html data/act.html data/icon.gif data/intro.html data/ref.html data/syn.html pref/chords.cfg.txt pref/TwidlitDuplicates.txt pref/TwidlitKeyEvents.txt pref/TwidlitKeyNames.txt pref/TwidlitKeyValues.txt pref/TwidlitLost.txt pref/TwidlitPersist.properties pref/TwidlitPreferences.txt pref/TwidlitUnprintables.txt
CLEAN=rm *.class *~ *.bak tmp
QUIET_CLEAN=${CLEAN} 2> /dev/null
QUIET_CLEAN_AND_BACK=${QUIET_CLEAN}; cd - > /dev/null

all: classes
jar: ./Twidlit/Twidlit.jar
io: pkp/io/Io.class pkp/io/LineReader.class pkp/io/SpacedPairReader.class
string: pkp/string/StringSource.class pkp/string/StringInt.class pkp/string/StringsInts.class pkp/string/StringsIntsBuilder.class
lookup: pkp/lookup/LookupBuilder.class pkp/lookup/LookupImplementation.class pkp/lookup/LookupSet.class pkp/lookup/LookupSetBuilder.class pkp/lookup/LookupTable.class pkp/lookup/LookupTableBuilder.class pkp/lookup/SharedIndex.class
ui: pkp/ui/ControlDialog.class pkp/ui/ControlWindow.class pkp/ui/ExtensionFileFilter.class pkp/ui/HtmlWindow.class pkp/ui/IntegerTextField.class pkp/ui/IntegerSetter.class pkp/ui/LabelComponentBox.class pkp/ui/PersistentDialog.class pkp/ui/PersistentFrame.class pkp/ui/PersistentMenuBar.class pkp/ui/ProgressWindow.class pkp/ui/SaveTextWindow.class pkp/ui/ScalePanel.class pkp/ui/SliderBuilder.class pkp/ui/TextWindow.class
util: pkp/util/Log.class pkp/util/Persist.class pkp/util/Persistent.class pkp/util/PersistentProperties.class pkp/util/Pref.class 
chars: pkp/chars/CharCounts.class pkp/chars/Counts.class pkp/chars/NGram.class pkp/chars/NGrams.class
source: pkp/source/ChordSource.class pkp/source/KeyPressListSource.class pkp/source/UniformSource.class
text: pkp/text/TextPanel.class
times: pkp/times/ChordTimes.class pkp/times/SortedChordTimes.class
twiddle: pkp/twiddle/Assignment.class pkp/twiddle/Chord.class pkp/twiddle/KeyMap.class pkp/twiddle/KeyPress.class pkp/twiddle/KeyPressList.class pkp/twiddle/Modifiers.class pkp/twiddle/ThumbKeys.class pkp/twiddle/Twiddle.class 
twiddler: pkp/twiddler/Cfg.class pkp/twiddler/Settings.class pkp/twiddler/SettingsWindow.class 
twidlit: pkp/twidlit/ChordMapper.class pkp/twidlit/CountsRangeSetter.class pkp/twidlit/Hand.class pkp/twidlit/ProgressPanel.class pkp/twidlit/TwiddlerWindow.class pkp/twidlit/Twidlit.class pkp/twidlit/TwidlitMenu.class
classes: io string lookup ui util chars source text times twiddle twiddler twidlit
clean:
	@cd data; ${QUIET_CLEAN_AND_BACK}
	@cd pref; ${QUIET_CLEAN_AND_BACK}
	@cd script; ${QUIET_CLEAN_AND_BACK}
	@cd pkp/io; ${QUIET_CLEAN_AND_BACK}
	@cd pkp/string; ${QUIET_CLEAN_AND_BACK}
	@cd pkp/lookup; ${QUIET_CLEAN_AND_BACK}
	@cd pkp/ui; ${QUIET_CLEAN_AND_BACK}
	@cd pkp/util; ${QUIET_CLEAN_AND_BACK}
	@cd pkp/chars; ${QUIET_CLEAN_AND_BACK}
	@cd pkp/source; ${QUIET_CLEAN_AND_BACK}
	@cd pkp/text; ${QUIET_CLEAN_AND_BACK}
	@cd pkp/times; ${QUIET_CLEAN_AND_BACK}
	@cd pkp/twiddle; ${QUIET_CLEAN_AND_BACK}
	@cd pkp/twiddler; ${QUIET_CLEAN_AND_BACK}
	@cd pkp/twidlit; ${QUIET_CLEAN_AND_BACK}
	@${QUIET_CLEAN} pref/TwidlitPreferences.txt classlist.txt TwidlitLog.txt TwidlitPersist.properties ||:
version:
	@script/incVersion.sh show data/about.html
major minor update:
	@cp data/about.html data/about.html~
	@script/incVersion.sh $@ data/about.html~ > data/about.html
	@unix2dos.exe data/about.html 2> /dev/null
   
%.class: %.java
	javac $<

pref/TwidlitPreferences.txt: data/ref.html script/makePrefs.sh
	@script/makePrefs.sh data/ref.html pref/TwidlitPreferences.tmp
	@diff pref/TwidlitPreferences.txt pref/TwidlitPreferences.tmp 2> /dev/null ||:
	@mv pref/TwidlitPreferences.tmp pref/TwidlitPreferences.txt
   
# list of the classes to jar, escaping the $s
classlist.txt: classes
	find . -iname \*.class | sed 's/\$$/\\$$/g' > $@

./Twidlit/Twidlit.jar: Manifest.txt ${JAR_DATA} classlist.txt
	jar cfm $@ Manifest.txt ${JAR_DATA} $(shell cat classlist.txt)

