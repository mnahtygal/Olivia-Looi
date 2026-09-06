import java.nio.file.*;
import java.util.regex.*;
public class CaptureFixtureCheck {
  public static void main(String[] args) throws Exception {
    Class<?> c=Class.forName("com.nahtygal.olivialooi.screenshots.CaptureFixtures");
    Object singleton=c.getField("INSTANCE").get(null);
    String inventory=Files.readString(Path.of(args[0]));
    Matcher ids=Pattern.compile("\"id\": \"([^\"]+)\"").matcher(inventory);
    int passed=0;
    while(ids.find()) {
      String id=ids.group(1);
      try { c.getMethod("create",String.class).invoke(singleton,id); passed++; }
      catch(java.lang.reflect.InvocationTargetException e) { throw new RuntimeException(id,e.getCause()); }
    }
    System.out.println("Fixture construction and production saver/codec checks passed: "+passed);
  }
}
