package overwatch.getPlugin.check.combat;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.EnumWrappers.EntityUseAction;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import overwatch.getPlugin.Overwatch;
import overwatch.getPlugin.check.Check;
import overwatch.getPlugin.packets.events.PacketUseEntityEvent;
import overwatch.getPlugin.utils.UtilTime;

public class KillAuraC extends Check
{
    private Map<UUID, Map.Entry<Integer, Long>> AimbotTicks;
    private Map<UUID, Double> Differences;
    private Map<UUID, Location> LastLocation;

    public KillAuraC(final Overwatch overwatch) {
        super("KillAuraC", "Kill Aura (Aimbot)", overwatch);
        this.AimbotTicks = new HashMap<UUID, Map.Entry<Integer, Long>>();
        this.Differences = new HashMap<UUID, Double>();
        this.LastLocation = new HashMap<UUID, Location>();
        this.setAutobanTimer(true);
    }

    @Override
    public void onEnable() {
    }

    @EventHandler
    public void UseEntity(final PacketUseEntityEvent e) {
        if (e.getAction() != EnumWrappers.EntityUseAction.ATTACK) {
            return;
        }
        if (!(e.getAttacked() instanceof Player)) {
            return;
        }
        final Player damager = e.getAttacker();
        if (damager.getAllowFlight()) {
            return;
        }
        Location from = null;
        final Location to = damager.getLocation();
        if (this.LastLocation.containsKey(damager.getUniqueId())) {
            from = this.LastLocation.get(damager.getUniqueId());
        }
        this.LastLocation.put(damager.getUniqueId(), damager.getLocation());
        int Count = 0;
        long Time = System.currentTimeMillis();
        double LastDifference = -111111.0;
        if (this.Differences.containsKey(damager.getUniqueId())) {
            LastDifference = this.Differences.get(damager.getUniqueId());
        }
        if (this.AimbotTicks.containsKey(damager.getUniqueId())) {
            Count = this.AimbotTicks.get(damager.getUniqueId()).getKey();
            Time = this.AimbotTicks.get(damager.getUniqueId()).getValue();
        }
        if (from == null || (to.getX() == from.getX() && to.getZ() == from.getZ())) {
            return;
        }
        final double Difference = Math.abs(to.getYaw() - from.getYaw());
        if (Difference == 0.0) {
            return;
        }
        if (Difference > 2.0) {
            this.dumplog(damager, "Difference: " + Difference);
            final double diff = Math.abs(LastDifference - Difference);
            if (diff < 3.0) {
                ++Count;
                this.dumplog(damager, "New Count: " + Count);
            }
        }
        this.Differences.put(damager.getUniqueId(), Difference);
        if (this.AimbotTicks.containsKey(damager.getUniqueId()) && UtilTime.elapsed(Time, 5000L)) {
            this.dumplog(damager, "Count Reset");
            Count = 0;
            Time = UtilTime.nowlong();
        }
        if (Count >= 10) {
            Count = 0;
            this.dumplog(damager, "Logged. Last Difference: " + Math.abs(to.getYaw() - from.getYaw()) + ", Count: " + Count);
            this.getOverwatch().logCheat(this, damager, "Aimbot", new String[0]);
        }
        this.AimbotTicks.put(damager.getUniqueId(), new AbstractMap.SimpleEntry<Integer, Long>(Count, Time));
    }
}