package us.abstracta.jmeter.javadsl.core;

import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import org.apache.jmeter.gui.JMeterGUIComponent;
import org.apache.jmeter.testbeans.gui.TestBeanGUI;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.collections.HashTree;
import us.abstracta.jmeter.javadsl.core.EmbeddedJmeterEngine.JMeterEnvironment;

/**
 * Provides the basic logic for all {@link DslTestElement}.
 * <p>
 * In particular it currently allows to set the name of the TestElement and abstracts building of
 * the tree only requiring, from subclasses, to implement the logic to build the JMeter TestElement.
 * The test element name is particularly useful for later reporting and statistics collection to
 * differentiate metrics for each test element.
 * <p>
 * Sub classes may overwrite {@link #buildTreeUnder} if they need additional logic (e.g: {@link
 * TestElementContainer}).
 *
 * @since 0.1
 */
public abstract class BaseTestElement implements DslTestElement {

  protected final String name;
  protected Class<? extends JMeterGUIComponent> guiClass;

  protected BaseTestElement(String name, Class<? extends JMeterGUIComponent> guiClass) {
    this.name = name;
    this.guiClass = guiClass;
  }

  @Override
  public HashTree buildTreeUnder(HashTree parent, BuildTreeContext context) {
    return parent.add(buildConfiguredTestElement());
  }

  protected TestElement buildConfiguredTestElement() {
    TestElement ret = buildTestElement();
    ret.setName(name);
    ret.setProperty(TestElement.GUI_CLASS, guiClass.getName());
    ret.setProperty(TestElement.TEST_CLASS, ret.getClass().getName());
    return ret;
  }

  protected abstract TestElement buildTestElement();

  @Override
  public void showInGui() {
    showTestElementInGui(buildConfiguredTestElement(), null);
  }

  protected void showTestElementInGui(TestElement testElement, Runnable closeListener) {
    try (JMeterEnvironment env = new JMeterEnvironment()) {
      // this is required for proper visualization of labels and messages from resources bundle
      env.initLocale();
      JMeterGUIComponent gui =
          guiClass == TestBeanGUI.class ? new TestBeanGUI(testElement.getClass())
              : guiClass.newInstance();
      gui.clearGui();
      gui.configure(testElement);
      gui.modifyTestElement(testElement);
      showFrameWith((Component) gui, name, 800, 600, closeListener);
    } catch (InstantiationException | IllegalAccessException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected void showFrameWith(Component content, String title, int width, int height,
      Runnable closeListener) {
    JFrame frame = new JFrame(title);
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    if (closeListener != null) {
      frame.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
          super.windowClosed(e);
          closeListener.run();
        }
      });
    }
    frame.setLocation(200, 200);
    frame.setSize(width, height);
    frame.add(content);
    frame.setVisible(true);
  }

}
