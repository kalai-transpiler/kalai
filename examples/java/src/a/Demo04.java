package a;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
public class Demo04 {
public static final HashMap<Character,Integer> getDigitsMap() {
return new DigitsMap().put('0', 0, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('1', 1, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('2', 2, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('3', 3, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('4', 4, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('5', 5, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('6', 6, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('7', 7, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('8', 8, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('9', 9, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('٠', 0, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('١', 1, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('٢', 2, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('٣', 3, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('٤', 4, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('٥', 5, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('٦', 6, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('٧', 7, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('٨', 8, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('٩', 9, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('০', 0, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('১', 1, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('২', 2, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('৩', 3, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('৪', 4, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('৫', 5, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('৬', 6, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('৭', 7, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('৮', 8, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put('৯', 9, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS);
}
static final HashMap<Character,Integer> digitsMap = a.Demo04.getDigitsMap();
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
HashMap<String,ArrayList<Character>> tmp1 = new HashMap<String,ArrayList<Character>>();
ArrayList<Character> tmp2 = new ArrayList<Character>();
tmp2.add('٠');
tmp2.add('١');
tmp2.add('٢');
tmp2.add('٣');
tmp2.add('٤');
tmp2.add('٥');
tmp2.add('٦');
tmp2.add('٧');
tmp2.add('٨');
tmp2.add('٩');
tmp1.put("ARABIC", tmp2);
ArrayList<Character> tmp3 = new ArrayList<Character>();
tmp3.add('০');
tmp3.add('১');
tmp3.add('২');
tmp3.add('৩');
tmp3.add('৪');
tmp3.add('৫');
tmp3.add('৬');
tmp3.add('৭');
tmp3.add('৮');
tmp3.add('৯');
tmp1.put("BENGALI", tmp3);
ArrayList<Character> tmp4 = new ArrayList<Character>();
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
tmp1.put("LATIN", tmp4);
final HashMap<String,ArrayList<Character>> m = tmp1;
return m;
}
static final HashMap<String,ArrayList<Character>> numberSystemsMap = a.Demo04.getNumberSystemsMap();
public static final HashMap<String,Character> getGroupingSeparatorsMap() {
HashMap<String,Character> tmp5 = new HashMap<String,Character>();
tmp5.put("ARABIC", '٬');
tmp5.put("BENGALI", ',');
tmp5.put("LATIN", ',');
return tmp5;
}
static final HashMap<String,Character> groupingSeparatorsMap = a.Demo04.getGroupingSeparatorsMap();
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
final ArrayList<Integer> separatorPositions = a.Demo04.getSeparatorPositions(numLength, groupingStrategy);
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
System.out.println(a.Demo04.parse("٥٠٣٠١"));
System.out.println(a.Demo04.parse("৫০৩০১"));
System.out.println(a.Demo04.parse("7,654,321"));
System.out.println(a.Demo04.parse("76,54,321"));
System.out.println(a.Demo04.format(7654321, "LATIN", "ON_ALIGNED_3_2"));
System.out.println(a.Demo04.format(7654321, "ARABIC", "ON_ALIGNED_3_3"));
System.out.println(a.Demo04.format(7654321, "BENGALI", "ON_ALIGNED_3_3"));
}
}
