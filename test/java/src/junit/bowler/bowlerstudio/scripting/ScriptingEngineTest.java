package junit.bowler.bowlerstudio.scripting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;

import java.util.Optional;

import org.junit.Test;

public class ScriptingEngineTest {

  @Test
  public void jsUrlToGistTest() {
    Optional<String> result
        = ScriptingEngine.urlToGist("https://gist.github"
        + ".com/Octogonapus/a7cd8acbffe43680c239e623713922be.js");

    assertTrue(result.isPresent());
    assertEquals("a7cd8acbffe43680c239e623713922be", result.get());
  }

  @Test
  public void gitUrlToGistTest() {
    Optional<String> result
        = ScriptingEngine.urlToGist("https://gist.github"
        + ".com/Octogonapus/a7cd8acbffe43680c239e623713922be.git");

    assertTrue(result.isPresent());
    assertEquals("a7cd8acbffe43680c239e623713922be", result.get());
  }

}
