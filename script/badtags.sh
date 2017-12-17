# Check tag associations between code and preference/properties files

# avoid Pref.init and sm_PREF_
PREF='Pref.get|_PREF\>'

echo --- Untargeted code tags
grep -iv persist code.tags | egrep -iv $PREF

# strip line back to tag
TO_TAG='sed -n s/^[^#]*#\.\([a-z0-9\.-]*\).*$/\1/pg'
# use sed to avoid end of line issues
LIST_TAGS='sed -n s/^\([a-z0-9\.-]*\)[^#]*$/\1/pg'

echo --- Code tags missing from preferences 
egrep $PREF code.tags | $TO_TAG | sort -u > pref.tags
for t in `$LIST_TAGS pref.tags | sort -u`; do
   if ! grep -q $t pref/twidlit.preferences; then
      echo $t
   fi
done

echo --- Code tags missing from persistence 
grep -i persist code.tags | $TO_TAG | sort -u > persist.tags
for t in `$LIST_TAGS persist.tags | sort -u`; do
   if ! grep -q $t pref/twidlit.properties; then
      echo $t
   fi
done

echo --- Preference tags missing from code
for t in `$LIST_TAGS pref/twidlit.preferences`; do
   if ! grep -q $t pref.tags; then
      echo $t
   fi
done
echo --- Persistent tags missing from code
for t in `$LIST_TAGS pref/twidlit.properties`; do
   if ! grep -q $t persist.tags; then
      if ! grep -q $t pref/ui.tags; then
         echo $t
      fi
   fi
done

echo --- UI tags missing from persist
# use sed to avoid end of line issues
for t in `$LIST_TAGS pref/ui.tags`; do
   if ! grep -q $t pref/twidlit.properties; then
      echo $t
   fi
done
