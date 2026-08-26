package rc55.mc.ic2cr.addon.mets;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = MoreElectricTools.MODID)
public final class MoreElectricToolsConfig implements ConfigData {
    MoreElectricToolsConfig() {
    }

    public static MoreElectricToolsConfig getInstance() {
        return MoreElectricTools.CONFIG_HOLDER.getConfig();
    }

    @ConfigEntry.Gui.RequiresRestart
    public boolean WeaponDamagesTerrain = true;
    @ConfigEntry.Gui.RequiresRestart
    public boolean EnableElectricFirstAidLifeSupportRecipe = true;
    
    public boolean EnableElectricNutritionSupplyCost = true;
    @ConfigEntry.Gui.RequiresRestart
    public boolean EnableOreGenerate = true;
    //@ConfigEntry.Gui.RequiresRestart
    //public boolean EnableMoreKineticGenerator = true;
    @ConfigEntry.Gui.RequiresRestart
    public boolean EnableMoreStirlingGenerator = false;
    @ConfigEntry.Gui.RequiresRestart
    public boolean EnableFastTESR = false;
    @ConfigEntry.Gui.RequiresRestart
    public boolean EnableLighterDynamicSource = true;
    @ConfigEntry.Gui.RequiresRestart
    public boolean EnableEUSlashBladeRecipe = true;

    
    public double AdvancedIridiumSwordBaseCost = 800d;
    
    public float AdvancedIridiumSwordBaseAttackDamage = 25f;

    
    public double NanoBowBaseCost = 300d;
    
    public float NanoBowMaxVelocity = 5.0f;

    
    public double ElectricNutritionSupplyCost = 200d;

    
    public double ElectricFirstAidLifeSupport = 10000d;

    
    public double PlasmaAirCannonBaseCost = 1000d;
    
    public double PlasmaAirCannonBaseDamage = 10d;

    
    public double HeavyQuantumSuitDamageEnergyCost = 10000d;
    
    public double AdvancedQuantumSuitDamageEnergyCost = 10000d;
    
    public double AdvancedQuantumSuitCureCost = 30000d;

    
    public double ElectricSubmachineGunCost = 100d;

    
    public int ElectricRocketLauncherInterval = 1500;
    
    public double ElectricRocketLauncherCost = 50000d;

    
    public double AdvancedElectricSubmachineGunCost = 5000d;

    
    public double TacticalLaserSubmachineGunCost = 10000d;

    
    public double ElectricPlasmaGunCost = 5000d;

    
    public double ForceFieldCost = 5000d;

    
    public double TachyonDisruptorCost = 50000d;

    
    public double LaserTowerCost = 250d;

    
    public double AdvancedLaserTowerCost = 2500d;
}
