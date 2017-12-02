package junit.bowler;

import org.junit.Test;

import com.neuronrobotics.imageprovider.OpenCVLoader;

public class OpenCVLoadingTest {

  @Test
  public void test() {

    OpenCVLoader.loadJNI();

  }

}
