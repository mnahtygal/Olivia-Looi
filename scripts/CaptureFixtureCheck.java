import java.nio.file.*;
import java.util.regex.*;
public class CaptureFixtureCheck {
  public static void main(String[] args) throws Exception {
    Class.forName("com.nahtygal.olivialooi.screenshots.FixtureRestorationChecks").getMethod("run").invoke(null);
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
    Object main=c.getMethod("create",String.class).invoke(singleton,"animal_sounds_main");
    Object mainRegistry=main.getClass().getMethod("getRegistry").invoke(main);
    if(mainRegistry.getClass().getMethod("consumeRestored",String.class).invoke(mainRegistry,"empty")!=null)
      throw new AssertionError("Main animal fixture should use production defaults");
    for(int attempt=0;attempt<2;attempt++) {
      Object selected=c.getMethod("create",String.class).invoke(singleton,"animal_sounds_selected");
      Object registry=selected.getClass().getMethod("getRegistry").invoke(selected);
      var consume=registry.getClass().getMethod("consumeRestored",String.class);
      Object animal=consume.invoke(registry,"animal");
      if(!"cow".equals(animal)) throw new AssertionError("Non-deterministic animal payload");
      Object feedback=consume.invoke(registry,"feedback");
      if(!Integer.valueOf(0).equals(feedback)) throw new AssertionError("Unexpected audio feedback request payload");
      if(!Boolean.TRUE.equals(consume.invoke(registry,"showWord"))) throw new AssertionError("Missing selected sound label payload");
    }
    Object pool=c.getMethod("create",String.class).invoke(singleton,"billiards_free_play_rack");
    Object registry=pool.getClass().getMethod("getRegistry").invoke(pool);
    var consume=registry.getClass().getMethod("consumeRestored",String.class);
    consume.invoke(registry,"table");
    if(!(consume.invoke(registry,"spokenIdentity") instanceof Long)) throw new AssertionError("Expected Long restoration payload");
    System.out.println("Animal main/selected raw primitive payload checks and Billiards Long payload check passed");
    System.out.println("Fixture construction and production saver/codec checks passed: "+passed);
  }
}
