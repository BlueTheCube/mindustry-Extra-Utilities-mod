package ExtraUtilities.ui;

import ExtraUtilities.ExtraUtilitiesMod;
import arc.Core;
import arc.func.Cons;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import mindustry.content.UnitTypes;
import mindustry.game.Gamemode;
import mindustry.game.Rules;
import mindustry.game.Team;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.io.MapIO;
import mindustry.maps.Map;
import mindustry.type.Category;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import java.io.IOException;

import static mindustry.Vars.*;

public class RogueLikeStart extends BaseDialog {
    public Map map;
    public Weaves weaves = Weaves.limit;
    public Difficult difficult = Difficult.normal;
    public String[] mapNames = {"MitoKenos", "YayaSitken"};
    public ObjectMap<String, TextureRegion> regionMap = new ObjectMap<>();

    private Rules rules;

    public RogueLikeStart() {
        super("start");
    }

    public void toShow(){
        //Map map;// = maps.loadInternalMap("MitoKenos");
        Map[] maps = new Map[mapNames.length];
        for(int i = 0; i < mapNames.length; i++) {
            try {
                maps[i] = MapIO.createMap(ExtraUtilitiesMod.EU.root.child("roguelike").child(mapNames[i] + ".msav"), false);
                TextureRegion region = Core.atlas.find("extra-utilities-" + mapNames[i]);
                if(region.found()) regionMap.put(maps[i].name(), region);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        map = maps[0];
//        try {
//            map = MapIO.createMap(ExtraUtilitiesMod.EU.root.child("roguelike").child("MitoKenos.msav"), false);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

        cont.clear();

        cont.add("由于地图文件较大，部分机型无法打开为正常情况").center();
        cont.row();
        cont.add("some models may not be able to open...").center();
        cont.row();

        cont.image(Tex.whiteui, Pal.accent).left().width(500f).height(3f).pad(4f).row();

        Table playMap = new Table();
        playMap.add("map").colspan(2);
        playMap.row();

        playMap.table(Tex.button, diff -> {
            for(int i = 0; i < mapNames.length; i++){
                int finalI = i;
                diff.button(mapNames[i], Styles.flatToggleMenut, () -> map = maps[finalI]).update(b -> b.setChecked(map == maps[finalI])).size(140f, mobile ? 44f : 54f);
                if((i + 1) % 3 == 0) diff.row();
            }
        });

        cont.add(playMap).row();
        cont.table(img -> {
            img.add(map.name()).row();
            if(regionMap.containsKey(map.name())) img.image(regionMap.get(map.name())).size(192);
            playMap.changed(() -> rebuildImg(img));
        }).row();

        Table selweave = new Table();
        selweave.add("weave").colspan(2);
        selweave.row();

        selweave.table(Tex.button, diff -> {
            int i = 0;
            for(Weaves w : Weaves.values()){
                i++;
                diff.button(w.toString(), Styles.flatToggleMenut, () -> weaves = w).update(b -> b.setChecked(weaves == w)).size(140f, mobile ? 44f : 54f);
                if(i % 2 == 0) diff.row();
            }
        });

        cont.add(selweave);

        cont.row();

        Table selmode = new Table();
        selmode.add("difficult").colspan(2);
        selmode.row();

        rules = map.applyRules(Gamemode.survival);

        selmode.table(Tex.button, diff -> {
            int i = 0;
            for(Difficult d : Difficult.values()){
                i++;
                diff.button(d.toString(), Styles.flatToggleMenut, () -> difficult = d).update(b -> b.setChecked(difficult == d)).size(140f, mobile ? 44f : 54f);
                if(i % 2 == 0) diff.row();
            }
        });

        cont.add(selmode);
        cont.row();
        cont.image(Tex.whiteui, Pal.accent).left().width(500f).height(3f).pad(4f).row();
        cont.pane(ds -> {
            Table dt = new Table();
            dt.add(difficult.description()).wrap().fillX().padLeft(10).width(500f).padTop(10).left();
            Table dw = new Table();
            if(weaves == Weaves.limit){
                dw.add(Core.bundle.format("eu-rogue.endWeave", difficult.end)).wrap().fillX().padLeft(10).width(500f).padTop(10).left();
            }
            selmode.changed(() -> rebuildShow(dt, dw));
            selweave.changed(() -> rebuildShow(dt, dw));
            ds.add(dt).row();
            ds.add(dw).row();

            ds.add(Core.bundle.get("eu-rogue-like-tip")).wrap().fillX().padLeft(10).width(500f).padTop(10);
        });

        buttons.clearChildren();

        addCloseButton();
        addCloseListener();
        buttons.button("@play", Icon.play, () -> {
            if(rules == null) return;

            difficult.apply(rules, weaves == Weaves.endless);
            control.playMap(map, rules, false);
            hide();
            ui.custom.hide();
        }).size(210f, 64f).update(b -> {
            if(map == null) {
                b.setDisabled(true);
                b.setText("Coming soon...");
            } else {
                b.setDisabled(false);
                b.setText("@play");
            }
        });

        show();
    }

    private void rebuildImg(Table img){
        img.clear();
        img.add(map.name()).row();
        if(regionMap.containsKey(map.name())) img.image(regionMap.get(map.name())).size(192);
    }

    private void rebuildShow(Table dt, Table dw){
        dt.clear();
        dt.add(difficult.description()).wrap().fillX().padLeft(10).width(500f).padTop(10).left();
        dw.clear();
        if(weaves == Weaves.limit){
            dw.add(Core.bundle.format("eu-rogue.endWeave", difficult.end)).wrap().fillX().padLeft(10).width(500f).padTop(10).left();
        }
    }

    public enum Difficult{
        easy(rules -> {
            rules.blockHealthMultiplier = 2;
            rules.blockDamageMultiplier = 1.5f;
            rules.buildCostMultiplier = 0.5f;
            defRule(rules);
        }, 40),
        normal(Difficult::defRule, 60),
        hard(rules -> {
            rules.blockDamageMultiplier = 0.8f;
            rules.buildCostMultiplier = 1.1f;
            rules.bannedBlocks.addAll(content.blocks().copy().removeAll(b -> b.category != Category.turret || b.size < 5));
            defRule(rules);
        }, 80),
        impossible(rules -> {
            rules.blockDamageMultiplier = 0.6f;
            rules.buildCostMultiplier = 1.2f;
            rules.deconstructRefundMultiplier = 0;
            rules.bannedBlocks.addAll(content.blocks().copy().removeAll(b -> b.category != Category.turret || b.size < 5));
            defRule(rules);
        }, 100);

        private final Cons<Rules> rules;
        private final int end;

        Difficult(Cons<Rules> rules, int end){
            this.rules = rules;
            this.end = end;
        }

        public Rules apply(Rules in, boolean endless){
            rules.get(in);
            if(!endless) in.winWave = end;
            return in;
        }

        @Override
        public String toString() {
            return Core.bundle.get("eu-rogue." + name() + ".name");
        }

        public String description() {
            return Core.bundle.get("eu-rogue." + name() + ".description");
        }

        public static void defRule(Rules rules){
            rules.bannedUnits.addAll(content.units());
            rules.bannedUnits.remove(UnitTypes.mono);
            rules.bannedUnits.remove(UnitTypes.poly);
            rules.teams.get(Team.crux).blockHealthMultiplier = 9999;
        }
    }

    public enum Weaves{
        limit,
        endless;

        @Override
        public String toString() {
            return Core.bundle.get("eu-rogue." + name() + ".name");
        }
    }
}
