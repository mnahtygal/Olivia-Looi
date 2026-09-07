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
    Class<?> stateType=Class.forName("androidx.compose.runtime.State");
    Class<?> intType=Class.forName("androidx.compose.runtime.MutableIntState");
    Class<?> longType=Class.forName("androidx.compose.runtime.MutableLongState");
    Object main=c.getMethod("create",String.class).invoke(singleton,"animal_sounds_main");
    Object mainRegistry=main.getClass().getMethod("getRegistry").invoke(main);
    if(mainRegistry.getClass().getMethod("consumeRestored",String.class).invoke(mainRegistry,"empty")!=null)
      throw new AssertionError("Main animal fixture should use production defaults");
    for(int attempt=0;attempt<2;attempt++) {
      Object selected=c.getMethod("create",String.class).invoke(singleton,"animal_sounds_selected");
      Object registry=selected.getClass().getMethod("getRegistry").invoke(selected);
      var consume=registry.getClass().getMethod("consumeRestored",String.class);
      Object animal=consume.invoke(registry,"animal");
      if(!"cow".equals(stateType.getMethod("getValue").invoke(animal))) throw new AssertionError("Non-deterministic animal state");
      Object feedback=consume.invoke(registry,"feedback");
      if(!intType.isInstance(feedback)) throw new AssertionError("Expected MutableIntState restore slot");
      if(!Integer.valueOf(0).equals(intType.getMethod("getIntValue").invoke(feedback))) throw new AssertionError("Unexpected audio feedback request");
      if(!Boolean.TRUE.equals(stateType.getMethod("getValue").invoke(consume.invoke(registry,"showWord")))) throw new AssertionError("Missing selected sound label");
    }
    Object pool=c.getMethod("create",String.class).invoke(singleton,"billiards_free_play_rack");
    Object registry=pool.getClass().getMethod("getRegistry").invoke(pool);
    var consume=registry.getClass().getMethod("consumeRestored",String.class);
    if(!stateType.isInstance(consume.invoke(registry,"table"))) throw new AssertionError("Expected MutableState codec restore slot");
    if(!longType.isInstance(consume.invoke(registry,"spokenIdentity"))) throw new AssertionError("Expected MutableLongState restore slot");
    System.out.println("Explicit MutableState, MutableIntState, and MutableLongState restore-slot checks passed");
    System.out.println("Fixture construction and production saver/codec checks passed: "+passed);
  }
}
