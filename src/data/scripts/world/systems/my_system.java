package data.scripts.world.systems;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;


public class my_system {
    public void generate(SectorAPI sector) {

        float planet1Dist= 4000f; //this is the distance the planet is from the star

        StarSystemAPI system = sector.createStarSystem("Valoria Star System");
        system.getLocation().set(9999, 19999); //position of the system on the map

        system.setBackgroundTextureFilename("graphics/backgrounds/AEGbackground.jpg");

        // create the star and generate the hyperspace anchor for this system
        PlanetAPI my_system_star = system.initStar("my_system_star ", // unique id for this star
                "black_hole", // id in planets.json
                300f, // radius (in pixels at default zoom)
                300); // corona radius, from star edge
        system.setLightColor(new Color(147,255,140)); // light color in entire system, affects all entities

        PlanetAPI planet1 = system.addPlanet("planet1",
                my_system_star, //what it's orbiting
                "Valoria", //name
                "terran", //the planet type (look in planets.json for more)
                900, //angle
                399f, //radius
                planet1Dist, //distance from star
                700f); //how many days to orbit
        planet1.setCustomDescriptionId("Astral Guardians Homeworld"); //for custom descriptions

        MarketAPI planet1_market = addMarketplace.addMarketplace("AEG", planet1,
                null, //connected entities, like stations
                "Valoria",
                10, //size
                new ArrayList<>(Arrays.asList( //these are conditions
                        Conditions.POPULATION_10,
                        Conditions.RUINS_VAST,
                        Conditions.ORE_RICH,
                        Conditions.FARMLAND_RICH )),
                new ArrayList<>(Arrays.asList( //industries
                        Industries.POPULATION,
                        Industries.MEGAPORT,
                        Industries.ORBITALSTATION,
                        Industries.MINING,
                        Industries.HIGHCOMMAND,
                        Industries.ORBITALWORKS,
                        Industries.TECHMINING
                )),
                new ArrayList<>(Arrays.asList(Submarkets.SUBMARKET_STORAGE, //and markets
                        Submarkets.GENERIC_MILITARY,
                        Submarkets.SUBMARKET_BLACK,
                        Submarkets.SUBMARKET_OPEN)),
                0.15f
        );

        system.autogenerateHyperspaceJumpPoints(true, true); //generates jump points

        HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin(); //these lines clear the hyperspace clouds around the system
        NebulaEditor editor = new NebulaEditor(plugin);
        float minRadius = plugin.getTileSize() * 2f;

        float radius = system.getMaxRadiusInHyperspace();
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f);
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f); 

        }
    }
