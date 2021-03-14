package a;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
public class Demo02 {
public static final HashMap<Character,Integer> getDigitsMap() {
HashMap<Character,Integer> tmp8 = new HashMap<Character,Integer>();
tmp8.put('٠', 0);
tmp8.put('١', 1);
tmp8.put('٢', 2);
tmp8.put('٣', 3);
tmp8.put('٤', 4);
tmp8.put('٥', 5);
tmp8.put('০', 0);
tmp8.put('٦', 6);
tmp8.put('১', 1);
tmp8.put('٧', 7);
tmp8.put('২', 2);
tmp8.put('٨', 8);
tmp8.put('৩', 3);
tmp8.put('٩', 9);
tmp8.put('৪', 4);
tmp8.put('৫', 5);
tmp8.put('৬', 6);
tmp8.put('৭', 7);
tmp8.put('৮', 8);
tmp8.put('৯', 9);
tmp8.put('0', 0);
tmp8.put('1', 1);
tmp8.put('2', 2);
tmp8.put('3', 3);
tmp8.put('4', 4);
tmp8.put('5', 5);
tmp8.put('6', 6);
tmp8.put('7', 7);
tmp8.put('8', 8);
tmp8.put('9', 9);
return tmp8;
}
static final HashMap<Character,Integer> digitsMap = a.Demo02.getDigitsMap();
public static final int parse(final String s) {
int result = 0;
final int strLength = s.length();
{
int i = 0;
while ((i < strLength)) {
{
final char digit = s.charAt(i);
if (digitsMap.containsKey(digit))
{
final int digitVal = digitsMap.get(digit);
result = ((10 * result) + digitVal);
}
}
i = (i + 1);
}
return result;
}
}
public static final HashMap<String,ArrayList<Character>> getNumberSystemsMap() {
HashMap<String,ArrayList<Character>> tmp9 = new HashMap<String,ArrayList<Character>>();
ArrayList<Character> tmp10 = new ArrayList<Character>();
tmp10.add('٠');
tmp10.add('١');
tmp10.add('٢');
tmp10.add('٣');
tmp10.add('٤');
tmp10.add('٥');
tmp10.add('٦');
tmp10.add('٧');
tmp10.add('٨');
tmp10.add('٩');
tmp9.put("ARABIC", tmp10);
ArrayList<Character> tmp11 = new ArrayList<Character>();
tmp11.add('0');
tmp11.add('1');
tmp11.add('2');
tmp11.add('3');
tmp11.add('4');
tmp11.add('5');
tmp11.add('6');
tmp11.add('7');
tmp11.add('8');
tmp11.add('9');
tmp9.put("LATIN", tmp11);
ArrayList<Character> tmp12 = new ArrayList<Character>();
tmp12.add('০');
tmp12.add('১');
tmp12.add('২');
tmp12.add('৩');
tmp12.add('৪');
tmp12.add('৫');
tmp12.add('৬');
tmp12.add('৭');
tmp12.add('৮');
tmp12.add('৯');
tmp9.put("BENGALI", tmp12);
final HashMap<String,ArrayList<Character>> m = tmp9;
return m;
}
static final HashMap<String,ArrayList<Character>> numberSystemsMap = a.Demo02.getNumberSystemsMap();
public static final HashMap<String,Character> getGroupingSeparatorsMap() {
HashMap<String,Character> tmp13 = new HashMap<String,Character>();
tmp13.put("ARABIC", '٬');
tmp13.put("LATIN", ',');
tmp13.put("BENGALI", ',');
return tmp13;
}
static final HashMap<String,Character> groupingSeparatorsMap = a.Demo02.getGroupingSeparatorsMap();
public static final ArrayList<Integer> getSeparatorPositions(final int numLength, final String groupingStrategy) {
ArrayList<Integer> result = new ArrayList<Integer>();
if (groupingStrategy.equals("NONE"))
{
return result;
}
else
{
if (groupingStrategy.equals("ON_ALIGNED_3_3"))
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
if (groupingStrategy.equals("ON_ALIGNED_3_2"))
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
if (groupingStrategy.equals("MIN_2"))
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
return result;
}
}
}
}
}
public static final String format(final int num, final String numberSystem, final String groupingStrategy) {
int i = num;
java.lang.StringBuffer result = new StringBuffer();
{
while (!(i == 0)) {
final int quotient = (i / 10);
final int remainder = (i % 10);
final ArrayList<Character> numberSystemDigits = numberSystemsMap.get(numberSystem);
final char localDigit = numberSystemDigits.get(remainder);
{
result.insert(0, localDigit);
i = quotient;
}
}
{
final char sep = groupingSeparatorsMap.get(numberSystem);
final int numLength = result.length();
final ArrayList<Integer> separatorPositions = a.Demo02.getSeparatorPositions(numLength, groupingStrategy);
final int numPositions = separatorPositions.size();
int idx = 0;
while ((idx < numPositions)) {
{
final int position = separatorPositions.get(idx);
result.insert(position, sep);
}
idx = (idx + 1);
}
}
return result.toString();
}
}
public static final void main(String[] args) {
System.out.println(a.Demo02.parse("٥٠٣٠١"));
System.out.println(a.Demo02.parse("৫০৩০১"));
System.out.println(a.Demo02.parse("7,654,321"));
System.out.println(a.Demo02.parse("76,54,321"));
System.out.println(a.Demo02.format(7654321, "LATIN", "ON_ALIGNED_3_2"));
System.out.println(a.Demo02.format(7654321, "ARABIC", "ON_ALIGNED_3_3"));
System.out.println(a.Demo02.format(7654321, "BENGALI", "ON_ALIGNED_3_3"));
}
}