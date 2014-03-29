/*
 * SpawnChecker.
 * 
 * (c) 2014 alalwww
 * https://github.com/alalwww
 * 
 * This mod is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL.
 * Please check the contents of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt
 * 
 * この MOD は、Minecraft Mod Public License (MMPL) 1.0 の条件のもとに配布されています。
 * ライセンスの内容は次のサイトを確認してください。 http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package net.awairo.mcmod.spawnchecker.client.mode.preset.config;

import static net.awairo.mcmod.spawnchecker.client.mode.preset.Options.*;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import net.awairo.mcmod.spawnchecker.client.common.OptionSet;
import net.awairo.mcmod.spawnchecker.client.mode.Mode.Option;
import net.awairo.mcmod.spawnchecker.client.mode.core.ModeConfig;
import net.awairo.mcmod.spawnchecker.client.mode.preset.SlimeChunkVisualizerMode;

/**
 * スライムチャンク可視化モードの設定.
 * 
 * @author alalwww
 */
public class SlimeChunkVisualizerConfig extends SkeletalConfig
{
    SlimeChunkVisualizerConfig(ModeConfig config)
    {
        super(config);

        setCategoryComment("preset mode: SlimeChunkVisualizer configurations.");
    }

    @Override
    protected String configurationCategory()
    {
        return SlimeChunkVisualizerMode.ID;
    }

    @Override
    protected List<OptionSet> defaultOptionSetList()
    {
        return ImmutableList.of(
                OptionSet.of(DISABLED),
                OptionSet.of(SLIME_CHUNK),
                OptionSet.of(FORCE_SLIME),
                OptionSet.of(FORCE_SLIME, FORCE_GUIDELINE),
                OptionSet.of(SLIME_CHUNK, FORCE_SLIME),
                OptionSet.of(SLIME_CHUNK, FORCE_SLIME, FORCE_GUIDELINE)
                );
    }

    @Override
    protected Set<Option> allOptions()
    {
        return ImmutableSet.of(
                DISABLED,
                SLIME_CHUNK,
                SLIME,
                GUIDELINE,
                FORCE_SLIME,
                FORCE_GUIDELINE
                );
    }

    @Override
    protected OptionSet defaultSelectedOptionSet()
    {
        return OptionSet.of(SLIME_CHUNK);
    }

}
