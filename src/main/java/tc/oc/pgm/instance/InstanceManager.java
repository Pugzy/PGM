package tc.oc.pgm.instance;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import tc.oc.pgm.api.PGM;

public class InstanceManager {

  private Logger logger;

  private File instancesFile;
  private FileConfiguration instancesFileConfiguration;

  private static InstanceManager instance;
  private ServerInstance defaultInstance;
  private Map<String, ServerInstance> instances;

  public static InstanceManager get() {
    return instance;
  }

  public InstanceManager(Logger logger, File instancesFile) {

    this.instancesFile = instancesFile;
    this.logger = logger;

    // Load saved servers

    // Set default server

    if (!instancesFile.exists()) {
      try {
        FileUtils.copyInputStreamToFile(PGM.get().getResource("instances.yml"), instancesFile);
      } catch (IOException e) {
        logger.log(Level.SEVERE, "Failed to create the instances.yml file", e);
        return;
      }
    }

    this.instancesFileConfiguration = YamlConfiguration.loadConfiguration(instancesFile);
    loadInstances();

    instance = this;
  }

  public void loadInstances() {
    List<ServerInstance> list = new ArrayList<>();
    for (String key : instancesFileConfiguration.getKeys("servers")) {

    }
    //    for (String key :
    //        instancesFileConfiguration.getConfigurationSection("instances").getKeys(false)) {
    //      Object obj = instancesFileConfiguration.get(key);
    //      //            ServerInstance serverInstance = new ServerInstance();
    //      //            instances.put()
    //    }
  }

  public ServerInstance getDefault() {
    return this.defaultInstance;
  }

  public int getInstanceSize() {
    return instances.size();
  }

  public void createInstance(String name) {
    ServerInstance instance = new ServerInstance(name, 32, false, false, null);
    instances.put(name, instance);
  }

  public void removeInstance(String name) {
    ServerInstance instance = instances.get(name);
    if (getDefault() != instance) {
      //
    }
  }
}
