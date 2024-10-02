package data.scripts.world;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import data.scripts.world.systems.my_system;

public class AEGNameGen{
    public static void initFactionRelationships(SectorAPI sector) {
        FactionAPI hegemony = sector.getFaction(Factions.HEGEMONY);
        FactionAPI tritachyon = sector.getFaction(Factions.TRITACHYON);
        FactionAPI pirates = sector.getFaction(Factions.PIRATES);
        FactionAPI kol = sector.getFaction(Factions.KOL);
        FactionAPI church = sector.getFaction(Factions.LUDDIC_CHURCH);
        FactionAPI path = sector.getFaction(Factions.LUDDIC_PATH);
        FactionAPI league = sector.getFaction(Factions.PERSEAN);
        FactionAPI AEG = sector.getFaction("AEG");

        AEG.setRelationship(path.getId(), RepLevel.SUSPICIOUS);
        AEG.setRelationship(hegemony.getId(), RepLevel.SUSPICIOUS);
        AEG.setRelationship(pirates.getId(), RepLevel.SUSPICIOUS);
        AEG.setRelationship(tritachyon.getId(), RepLevel.FAVORABLE);
        AEG.setRelationship(church.getId(), RepLevel.SUSPICIOUS);
        AEG.setRelationship(kol.getId(), RepLevel.SUSPICIOUS);
        AEG.setRelationship(league.getId(), RepLevel.SUSPICIOUS);

    }

    public void generate(SectorAPI sector) {

            initFactionRelationships(sector);
            new my_system().generate(sector);


    }
}
