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

package net.awairo.mcmod.spawnchecker.client.mode.core;

import static com.google.common.base.Preconditions.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import net.awairo.mcmod.common.v1.util.config.ConfigCategory;
import net.awairo.mcmod.common.v1.util.config.Prop;
import net.awairo.mcmod.spawnchecker.SpawnChecker;
import net.awairo.mcmod.spawnchecker.client.common.ModeConfig;
import net.awairo.mcmod.spawnchecker.client.common.OptionSet;
import net.awairo.mcmod.spawnchecker.client.mode.Mode;

/**
 * モードの設定.
 * 
 * @author alalwww
 */
public abstract class ModeConfigChild extends ConfigCategory
{
    private static final Logger LOGGER = LogManager.getLogger(SpawnChecker.MOD_ID);

    private static final Pattern QUOTED_PATTERN = Pattern.compile("^\"([^\"]+)\"$");
    private static final String SEPARATOR = ",";

    private final Prop optionSetListProp;
    private final Prop selectedOptionProp;

    private ImmutableList<OptionSet> optionSetList;
    private OptionSet selected;

    /**
     * Constructor.
     * 
     * @param config 設定
     */
    protected ModeConfigChild(ModeConfig config)
    {
        super(config);

        // カンマ区切りオプションID文字列のリスト
        final List<String> defStrList = toStringList(defaultOptionSetList());

        // 設定で使う配列に変換
        final String[] defStrArray = defStrList.toArray(new String[defStrList.size()]);
        checkState(!defStrList.isEmpty(), "default option set list is empty");

        // 初期選択されるオプションセット
        final String optSetDef = join(defaultSelectedOptionSet());
        checkState(!Strings.isNullOrEmpty(optSetDef), "default option set is empty");

        // デフォルトの一覧にデフォルトの選択値がなければNG
        final int selectedIndexDef = defStrList.indexOf(optSetDef);
        checkState(selectedIndexDef >= 0, "not found default option set from set list.");

        // 設定ロード
        optionSetListProp = getListOf("optionSetList", defStrArray)
                .comment("quoted and comma separated value of mode.option ID set.\n"
                        + String.format("(default: %s)", Arrays.toString(defStrArray)));

        selectedOptionProp = getValueOf("selectedOption", selectedIndexDef)
                .comment("last selected option set list index.\n"
                        + String.format("(min: 0, max: optionSetList size - 1, default: %s)", selectedIndexDef));

        initialize(defStrList, defStrArray, selectedIndexDef);
    }

    /** 設定不備があったら初期値に戻し、オプションセットの一覧と初期選択のオプションセットをフィールドに設定. */
    private void initialize(List<String> defOptList, String[] defOptListArray, int defIndex)
    {
        // やだこのメソッド、めっちゃ泥臭い…

        List<String> list = Lists.newArrayList(optionSetListProp.getStringList());

        // オプションセットの設定が全部正しいかチェック
        boolean hasIllegalValue = false;
        for (Iterator<String> ite = list.iterator(); ite.hasNext();)
        {
            final ImmutableList<String> ids = split(ite.next());
            if (!ids.isEmpty())
            {
                try
                {
                    final OptionSet optionSet = createOptionSetBy(ids);
                    if (ids.size() == optionSet.size())
                        continue;
                }
                catch (IllegalStateException ignore)
                {
                    // 空要素のOptionSetは作れないため例外が発生するが、無視する
                }
            }

            // あ、このIDリストの設定壊れてる！
            LOGGER.warn("{} is illegal configuration value of \"{}.optionSetList\".", ids, categoryName);
            ite.remove();
            hasIllegalValue = true;
        }

        // 不正値があったら不正値を削除した状態で更新
        if (hasIllegalValue)
            optionSetListProp.setList(list.toArray(new String[list.size()]));

        // 選択するオプションセットID文字列
        final String selectedSetIds;

        if (hasElementFor(list, selectedOptionProp.getInt()))
        {
            // 設定からロードしたリストから選択できた
            selectedSetIds = list.get(selectedOptionProp.getInt());
        }
        else if (hasElementFor(list, defIndex))
        {
            // 初期選択値でなら選択できるので、前回選択値をリセット

            LOGGER.warn("selectedOption({}) is illegal value. Reset to default. (category:{})",
                    selectedOptionProp.getInt(), categoryName);

            selectedOptionProp.set(defIndex);
            selectedSetIds = list.get(defIndex);
        }
        else if (hasElementFor(defOptList, selectedOptionProp.getInt()))
        {
            // 初期設定のオプションセット一覧からなら選択できるので、オプションセット一覧をリセット

            LOGGER.warn("optionSetList({}) is illegal value. Resets to default. (category:{})",
                    list, categoryName);

            optionSetListProp.setList(defOptListArray);
            selectedSetIds = defOptList.get(selectedOptionProp.getInt());
            list = defOptList;
        }
        else
        {
            // どっちもおかしいので両方リセット
            LOGGER.warn("optionSetList({}) is illegal value. Resets to default. (category:{})",
                    list, categoryName);
            LOGGER.warn("selectedOption({}) is illegal value. Reset to default. (category:{})",
                    selectedOptionProp.getInt(), categoryName);

            selectedOptionProp.set(defIndex);
            optionSetListProp.setList(defOptListArray);
            selectedSetIds = defOptList.get(defIndex);
            list = defOptList;
        }

        // 初期選択と設定から生成された選択候補のリストを生成
        selected = createOptionSetBy(split(selectedSetIds));

        optionSetList = ImmutableList.copyOf(Lists.transform(list, new Function<String, OptionSet>()
        {
            @Override
            public OptionSet apply(String input)
            {
                return createOptionSetBy(split(input));
            }
        }));
    }

    /**
     * 初期値を取得します.
     * 
     * @return オプションセット一覧の初期値
     */
    protected abstract List<OptionSet> defaultOptionSetList();

    /**
     * 初期値を取得します.
     * 
     * @return 最初に選択するオプションセット
     */
    protected abstract OptionSet defaultSelectedOptionSet();

    /**
     * IDを元にオプションセットを生成します.
     * 
     * @param ids オプションID
     * @return オプションセット
     */
    protected abstract OptionSet createOptionSetBy(ImmutableList<String> ids);

    // --------------------------------------------

    /**
     * 現在選択されているオプションセットを取得します.
     * 
     * @return 現在選択されているオプションセット
     */
    public OptionSet selectedOptionSet()
    {
        return selected;
    }

    /**
     * オプションセットの一覧を取得します.
     * 
     * @return オプションセットのリスト
     */
    public ImmutableList<OptionSet> getOptionSetList()
    {
        return optionSetList;
    }

    /**
     * 選択したオプションセットを設定します.
     * 
     * @param newOptionSet 選択したオプションセット
     */
    public void setSelectedOptionSet(OptionSet newOptionSet)
    {
        checkArgument(optionSetList.contains(newOptionSet));

        selectedOptionProp.set(optionSetList.indexOf(newOptionSet));
        selected = newOptionSet;
    }

    /**
     * オプションセットのリストを設定します.
     * 
     * @param newList オプションセットのリスト
     */
    public void setOptionSetList(List<OptionSet> newList)
    {
        checkArgument(newList.contains(selected));

        optionSetListProp.setList(toArray(newList));
        optionSetList = ImmutableList.copyOf(newList);
    }

    // --------------------------------------

    /** @return true はリストが指定したインデックスに要素を持っている */
    private static boolean hasElementFor(List<String> list, int index)
    {
        return index >= 0 && index < list.size() && list.get(index) != null;
    }

    /** @return オプションセットをカンマ区切りのオプションID文字列に変換したリスト */
    private static List<String> toStringList(List<OptionSet> optionSets)
    {
        return Lists.transform(optionSets, new Function<OptionSet, String>()
        {
            @Override
            public String apply(OptionSet input)
            {
                return join(input);
            }
        });
    }

    /**
     * オプションID文字列の一覧にして返します.
     * 
     * @param optionSetStrings オプションID文字列
     * @return オプションID文字列の一覧
     */
    private static String[] toArray(List<OptionSet> newList)
    {
        final List<String> list = toStringList(newList);
        return list.toArray(new String[list.size()]);
    }

    /**
     * オプションIDをカンマ区切りで接続して返します
     * 
     * @param optionSet オプション
     * @return IDをカンマで区切って接続した文字列
     */
    private static String join(OptionSet optionSet)
    {
        return join(Collections2.transform(optionSet, new Function<Mode.Option, String>()
        {
            @Override
            public String apply(Mode.Option input)
            {
                return input.id();
            }
        }));
    }

    /**
     * @param optionSetIds モードオプションのIDの一覧
     * @return IDをカンマで区切って接続した文字列
     */
    private static String join(Collection<String> optionSetIds)
    {
        // ForgeのConfigurationはリスト値はダブルクオートで括らないと記号などを含められないため括ってる
        return String.format("\"%s\"", Joiner
                .on(SEPARATOR)
                .join(optionSetIds));
    }

    /** @return 引数の文字列をカンマで分割した不変のリスト */
    private static ImmutableList<String> split(String optionSetIds)
    {
        final Matcher m = QUOTED_PATTERN.matcher(optionSetIds);
        if (!m.matches())
            return ImmutableList.of();

        // ダブルクオートを除外した値をカンマで分割
        return ImmutableList.copyOf(Splitter.on(SEPARATOR).split(m.group(1)));
    }

}
