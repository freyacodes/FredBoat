/*
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package fredboat.util;

import fredboat.commandmeta.abs.CommandContext;
import net.dv8tion.jda.core.entities.*;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ArgumentUtil {

    public static final int FUZZY_RESULT_LIMIT = 10;

    private ArgumentUtil() {
    }

    /**
     * Search a collection of users for a search term.
     *
     * @param users       list of users to be searched
     * @param term        the term the user shall be searched for
     * @param includeBots false to exclude bots from results
     */
    @CheckReturnValue
    public static List<User> fuzzyUserSearch(Collection<User> users, String term, boolean includeBots) {
        ArrayList<User> list = new ArrayList<>();

        String searchTerm = term.toLowerCase();

        for (User user : users) {
            if ((user.getName().toLowerCase() + "#" + user.getDiscriminator()).contains(searchTerm)
                    || searchTerm.contains(user.getId())) {

                if (!includeBots && user.isBot()) continue;
                list.add(user);
            }
        }

        return list;
    }

    /**
     * Search a list of users for exactly one match. If there are no matches / multiple ones, the context will be
     * informed accordingly informed, so that the caller of this method has nothing to do and should return.
     *
     * @param users       list of users to be searched
     * @param context     the context of this search, where output may be directed if non / multiple results are found
     * @param term        the term the user shall be searched for
     * @param includeBots false to exclude bots from results
     */

    @CheckReturnValue
    public static Optional<User> checkSingleFuzzyUserSearchResult(Collection<User> users, CommandContext context, String term, boolean includeBots) {
        List<User> found = fuzzyUserSearch(users, term, includeBots);
        switch (found.size()) {
            case 0:
                context.reply(context.i18nFormat("fuzzyNothingFound", term));
                return Optional.empty();
            case 1:
                return Optional.of(found.get(0));
            default:
                context.reply(context.i18n("fuzzyMultiple") + "\n"
                        + formatFuzzyUserResult(found, FUZZY_RESULT_LIMIT, 1900));
                return Optional.empty();
        }
    }

    @CheckReturnValue
    public static List<Member> fuzzyMemberSearch(Guild guild, String term, boolean includeBots) {
        ArrayList<Member> list = new ArrayList<>();

        term = term.toLowerCase();

        for (Member mem : guild.getMembers()) {
            if ((mem.getUser().getName().toLowerCase() + "#" + mem.getUser().getDiscriminator()).contains(term)
                    || (mem.getEffectiveName().toLowerCase().contains(term))
                    || term.contains(mem.getUser().getId())) {

                if (!includeBots && mem.getUser().isBot()) continue;
                list.add(mem);
            }
        }

        return list;
    }

    @CheckReturnValue
    public static List<Role> fuzzyRoleSearch(Guild guild, String term) {
        ArrayList<Role> list = new ArrayList<>();

        term = term.toLowerCase();

        for (Role role : guild.getRoles()) {
            if ((role.getName().toLowerCase()).contains(term)
                    || term.contains(role.getId())) {
                list.add(role);
            }
        }

        return list;
    }

    @CheckReturnValue
    public static Member checkSingleFuzzyMemberSearchResult(CommandContext context, String term) {
        return checkSingleFuzzyMemberSearchResult(context, term, false);
    }

    @CheckReturnValue
    public static Member checkSingleFuzzyMemberSearchResult(CommandContext context, String term, boolean includeBots) {
        List<Member> list = fuzzyMemberSearch(context.guild, term, includeBots);

        switch (list.size()) {
            case 0:
                context.reply(context.i18nFormat("fuzzyNothingFound", term));
                return null;
            case 1:
                return list.get(0);
            default:
                context.reply(context.i18n("fuzzyMultiple") + "\n"
                        + formatFuzzyMemberResult(list, FUZZY_RESULT_LIMIT, 1900));
                return null;
        }
    }

    @CheckReturnValue
    @Nonnull
    public static String formatFuzzyMemberResult(@Nonnull List<Member> members, int maxLines, int maxLength) {
        return formatFuzzyUserOrMemberResult(UserWithOptionalNick.fromMembers(members), maxLines, maxLength);
    }

    @CheckReturnValue
    @Nonnull
    public static String formatFuzzyUserResult(@Nonnull List<User> users, int maxLines, int maxLength) {
        return formatFuzzyUserOrMemberResult(UserWithOptionalNick.fromUsers(users), maxLines, maxLength);
    }

    /**
     * Format a list of members as a text block, usually a result from a fuzzy search. The list should not be empty.
     *
     * @param maxLines  How many results to display. Pass Integer.MAX_VALUE to use as many as possible.
     * @param maxLength How many characters the resulting string may have as a max.
     */
    @CheckReturnValue
    @Nonnull
    public static String formatFuzzyUserOrMemberResult(@Nonnull List<UserWithOptionalNick> list, int maxLines, int maxLength) {

        List<UserWithOptionalNick> toDisplay;
        boolean addDots = false;
        if (list.size() > maxLines) {
            addDots = true;
            toDisplay = list.subList(0, maxLines);
        } else {
            toDisplay = new ArrayList<>(list);
        }

        int idPadding = toDisplay.stream()
                .mapToInt(member -> member.getUser().getId().length())
                .max()
                .orElse(0);
        int namePadding = toDisplay.stream()
                .mapToInt(member -> TextUtils.escapeBackticks(member.getUser().getName()).length())
                .max()
                .orElse(0)
                + 5;//for displaying discrim
        int nickPadding = toDisplay.stream()
                .mapToInt(member -> member.getNick().map(String::length).orElse(0))
                .max()
                .orElse(0);

        List<String> lines = toDisplay.stream()
                .map(member -> TextUtils.padWithSpaces(member.getUser().getId(), idPadding, true)
                        + " " + TextUtils.padWithSpaces(TextUtils.escapeBackticks(member.getUser().getName())
                        + "#" + member.getUser().getDiscriminator(), namePadding, false)
                        + " " + TextUtils.escapeBackticks(TextUtils.padWithSpaces(member.getNick().orElse(""), nickPadding, false))
                        + "\n")
                .collect(Collectors.toList());


        StringBuilder sb = new StringBuilder(TextUtils.padWithSpaces("Id", idPadding + 1, false)
                + TextUtils.padWithSpaces("Name", namePadding + 1, false)
                + TextUtils.padWithSpaces("Nick", nickPadding + 1, false) + "\n");

        int textBlockLength = 8;
        String dotdotdot = "[...]";
        for (int i = 0; i < toDisplay.size(); i++) {
            String line = lines.get(i);
            if (sb.length() + line.length() + dotdotdot.length() + textBlockLength < maxLength) {
                sb.append(line);
            } else {
                sb.append(dotdotdot);
                addDots = false; //already added
                break;
            }
        }
        if (addDots) {
            sb.append(dotdotdot);
        }

        return TextUtils.asCodeBlock(sb.toString());
    }

    /**
     * Processes a list of mentionables (roles / users).
     * Replies in the context of there are none / more than one mentionable and returns null, otherwise returns the
     * single mentionable.
     */
    @CheckReturnValue
    @Nullable
    public static IMentionable checkSingleFuzzySearchResult(@Nonnull List<IMentionable> list,
                                                            @Nonnull CommandContext context, @Nonnull String term) {
        switch (list.size()) {
            case 0:
                context.reply(context.i18nFormat("fuzzyNothingFound", term));
                return null;
            case 1:
                return list.get(0);
            default:
                StringBuilder searchResults = new StringBuilder();
                int i = 0;
                for (IMentionable mentionable : list) {
                    if (i == FUZZY_RESULT_LIMIT) break;

                    if (mentionable instanceof Member) {
                        Member member = (Member) mentionable;
                        searchResults.append("\nUSER ")
                                .append(member.getUser().getId())
                                .append(" ")
                                .append(member.getEffectiveName());
                    } else if (mentionable instanceof Role) {
                        Role role = (Role) mentionable;
                        searchResults.append("\nROLE ")
                                .append(role.getId())
                                .append(" ")
                                .append(role.getName());
                    } else {
                        throw new IllegalArgumentException("Expected Role or Member, got " + mentionable);
                    }
                    i++;
                }

                if (list.size() > FUZZY_RESULT_LIMIT) {
                    searchResults.append("\n[...]");
                }

                context.reply(context.i18n("fuzzyMultiple") + "\n"
                        + TextUtils.asCodeBlock(searchResults.toString()));
                return null;
        }
    }

    /**
     * Helper method to combine all the options from command into a single String.
     * Will call trim on each combine.
     *
     * @param args Command arguments.
     * @return String object of the combined args or empty string.
     */
    public static String combineArgs(@Nonnull String[] args) {
        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            sb.append(arg.trim());
        }
        return sb.toString().trim();
    }

    private static class UserWithOptionalNick {

        public static List<UserWithOptionalNick> fromUsers(Collection<User> users) {
            return users.stream().map(UserWithOptionalNick::new).collect(Collectors.toList());
        }

        public static List<UserWithOptionalNick> fromMembers(Collection<Member> members) {
            return members.stream().map(UserWithOptionalNick::new).collect(Collectors.toList());
        }

        private final User user;
        @Nullable
        private final String nick;

        public UserWithOptionalNick(User user) {
            this.user = user;
            this.nick = null;
        }

        public UserWithOptionalNick(Member member) {
            this.user = member.getUser();
            this.nick = member.getNickname();
        }

        public User getUser() {
            return user;
        }

        public Optional<String> getNick() {
            return Optional.ofNullable(nick);
        }
    }
}
