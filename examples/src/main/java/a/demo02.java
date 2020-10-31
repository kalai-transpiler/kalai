package a;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
public class demo02 {
public static final HashMap<Character,Integer> getDigitsMap() {
final HashMap tmp1 = new HashMap();
tmp1.put('٠', 0);
tmp1.put('١', 1);
tmp1.put('٢', 2);
tmp1.put('٣', 3);
tmp1.put('٤', 4);
tmp1.put('٥', 5);
tmp1.put('০', 0);
tmp1.put('٦', 6);
tmp1.put('১', 1);
tmp1.put('٧', 7);
tmp1.put('২', 2);
tmp1.put('٨', 8);
tmp1.put('৩', 3);
tmp1.put('٩', 9);
tmp1.put('৪', 4);
tmp1.put('৫', 5);
tmp1.put('৬', 6);
tmp1.put('৭', 7);
tmp1.put('৮', 8);
tmp1.put('৯', 9);
tmp1.put('0', 0);
tmp1.put('1', 1);
tmp1.put('2', 2);
tmp1.put('3', 3);
tmp1.put('4', 4);
tmp1.put('5', 5);
tmp1.put('6', 6);
tmp1.put('7', 7);
tmp1.put('8', 8);
tmp1.put('9', 9);
final HashMap<Character,Integer> m = tmp1;
return m;
}
static final HashMap<Character,Integer> digitsMap = getDigitsMap();
public static final Integer parse(final String s) {
int result = 0;
final int strLength = s.length();
{
int i = 0;
while ((i < strLength)) {
{
final char digit = s.charAt(i);
if (digitsMap.containsKey(digit))
{
final Integer digitVal = digitsMap.get(digit);
result = ((10 * result) + digitVal);
}
}
i = ++i;
}
return result;
}
}
public static final HashMap<String,ArrayList<Character>> getNumberSystemsMap() {
final HashMap tmp2 = new HashMap();
final ArrayList tmp3 = new ArrayList();
tmp3.add('٠');
tmp3.add('١');
tmp3.add('٢');
tmp3.add('٣');
tmp3.add('٤');
tmp3.add('٥');
tmp3.add('٦');
tmp3.add('٧');
tmp3.add('٨');
tmp3.add('٩');
tmp2.put("ARABIC", tmp3);
final ArrayList tmp4 = new ArrayList();
tmp4.add('0');
tmp4.add('1');
tmp4.add('2');
tmp4.add('3');
tmp4.add('4');
tmp4.add('5');
tmp4.add('6');
tmp4.add('7');
tmp4.add('8');
tmp4.add('9');
tmp2.put("LATIN", tmp4);
final ArrayList tmp5 = new ArrayList();
tmp5.add('০');
tmp5.add('১');
tmp5.add('২');
tmp5.add('৩');
tmp5.add('৪');
tmp5.add('৫');
tmp5.add('৬');
tmp5.add('৭');
tmp5.add('৮');
tmp5.add('৯');
tmp2.put("BENGALI", tmp5);
final HashMap<String,ArrayList<Character>> m = tmp2;
return m;
}
static final HashMap<String,ArrayList<Character>> numberSystemsMap = getNumberSystemsMap();
public static final HashMap<String,Character> getGroupingSeparatorsMap() {
final HashMap tmp6 = new HashMap();
tmp6.put("ARABIC", '٬');
tmp6.put("LATIN", ',');
tmp6.put("BENGALI", ',');
final HashMap<String,Character> m = tmp6;
return m;
}
static final HashMap<String,Character> groupingSeparatorsMap = getGroupingSeparatorsMap();
public static final ArrayList<Integer> getSeparatorPositions(final Integer numLength, final String groupingStrategy) {
final ArrayList<Integer> tmp7 = new ArrayList<Integer>();
ArrayList<Integer> result = tmp7;
if ((groupingStrategy == "NONE"))
{
return result;
}
else
{
if ((groupingStrategy == "ON_ALIGNED_3_3"))
{
int i = (numLength - 3);
{
while ((0 < i)) {
result.add(i);
i = (i - 3);
}
return result;
}
}
else
{
if ((groupingStrategy == "ON_ALIGNED_3_2"))
{
int i = (numLength - 3);
{
while ((0 < i)) {
result.add(i);
i = (i - 2);
}
return result;
}
}
else
{
if ((groupingStrategy == "MIN_2"))
{
if ((numLength <= 4))
{
return result;
}
else
{
int i = (numLength - 3);
{
while ((0 < i)) {
result.add(i);
i = (i - 3);
}
return result;
}
}
}
else
{
if (true)
{
return result;
}
else
{
return null;
}
}
}
}
}
}
public static final String format(final Integer num, final String numberSystem, final String groupingStrategy) {
int i = num;
final StringBuffer result = new StringBuffer();
{
while (!(i == 0)) {
final Integer quotient = (i / 10);
final Integer remainder = (i % 10);
final ArrayList<Character> numberSystemDigits = numberSystemsMap.get(numberSystem);
final Character localDigit = numberSystemDigits.get(remainder);
{
result.insert(0, localDigit);
i = quotient;
}
}
{
final char sep = groupingSeparatorsMap.get(numberSystem);
final int numLength = result.length();
final ArrayList<Integer> separatorPositions = getSeparatorPositions(numLength, groupingStrategy);
final int numPositions = separatorPositions.size();
int idx = 0;
while ((idx < numPositions)) {
{
final int position = separatorPositions.get(idx);
result.insert(position, sep);
}
idx = ++idx;
}
}
return result.toString();
}
}
}