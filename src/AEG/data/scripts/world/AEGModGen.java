package data.scripts.world;

import java.awt.Color;
import java.util.List;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CrewXPLevel;
import com.fs.starfarer.api.fleet.FleetMemberType;

@SuppressWarnings("unchecked")
public class AEGModGen extends SectorGen {

    public void generate(SectorAPI sector) {

        StarSystemAPI system = sector.getStarSystem("Corvus");

        SectorEntityToken token = system.createToken(15000, 0);
        system.addSpawnPoint(new AEGspawnpoint(sector, system, 30, 1, token));
        token = system.createToken(15000, -15000);
                system.addSpawnPoint(new AEGspawnpoint(sector, system, 14, 1, token));

        FactionAPI AEG = sector.getFaction("Astral Explorer Guild");

                AEG.setRelationship("hegemony", 0);
        AEG.setRelationship("tritachyon", 0);
        AEG.setRelationship("pirates", 0);
        AEG.setRelationship("independent", 1);
        AEG.setRelationship("player", 0);
    }
}