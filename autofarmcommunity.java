package l2f.gameserver.autofarm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import l2f.gameserver.data.htm.HtmCache;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.Skill;
import l2f.gameserver.network.serverpackets.ShowBoard;

public class AutoFarmCommunity
{
    public Player self;

    private void showHtml(String html)
    {
        if (self == null)
            return;

        ShowBoard.separateAndSend(html, self);
    }

    public void start()
    {
        if (self == null)
            return;

        if (self.isAutoFarm())
        {
            self.sendMessage("Auto Farm já está ativado.");
            return;
        }

        self.startAutoFarm();
        self.sendMessage("Auto Farm ativado.");

        back();
    }

    public void stop()
    {
        if (self == null)
            return;

        if (!self.isAutoFarm())
        {
            self.sendMessage("Auto Farm já está desativado.");
            return;
        }

        self.stopAutoFarm();
        self.sendMessage("Auto Farm desativado.");

        back();
    }

    public void toggle()
    {
        if (self == null)
            return;

        if (self.isAutoFarm())
            stop();
        else
            start();
    }

    public void skill1()
    {
        showSkillList(1, 0);
    }

    public void skill2()
    {
        showSkillList(2, 0);
    }

    public void skill3()
    {
        showSkillList(3, 0);
    }

    public void skill1page2()
    {
        showSkillList(1, 1);
    }

    public void skill2page2()
    {
        showSkillList(2, 1);
    }

    public void skill3page2()
    {
        showSkillList(3, 1);
    }

    private void showSkillList(int slot, int page)
    {
        if (self == null)
            return;

        StringBuilder html = new StringBuilder();

        html.append("<html noscrollbar>");
        html.append("<title>Auto Farm - Seleção de Skill</title>");
        html.append("<body><br>");

        html.append("<center>");
        html.append("<font name=\"hs12\" color=\"LEVEL\">");
        html.append("SELECIONE A SKILL ");
        html.append(slot);
        html.append("</font>");
        html.append("</center>");

        html.append("<br>");

        /*
         * SEM SKILL
         */
        html.append("<center>");
        html.append("<button value=\"SEM SKILL\" ");
        html.append("action=\"bypass _bbsscripts;l2f.gameserver.autofarm.AutoFarmCommunity:select ");
        html.append(slot);
        html.append(" 0\" ");
        html.append("width=150 height=30>");
        html.append("</center>");

        html.append("<br><br>");

        List<Skill> skills = new ArrayList<Skill>(self.getAllSkills());

        Collections.sort(skills, new Comparator<Skill>()
        {
            @Override
            public int compare(Skill a, Skill b)
            {
                if (a == null && b == null)
                    return 0;

                if (a == null)
                    return 1;

                if (b == null)
                    return -1;

                String nameA = a.getName();
                String nameB = b.getName();

                if (nameA == null)
                    nameA = "";

                if (nameB == null)
                    nameB = "";

                return nameA.compareToIgnoreCase(nameB);
            }
        });

        /*
         * Montamos uma lista somente com skills válidas
         * para não contar passivas/skills sem nome.
         */
        List<Skill> validSkills = new ArrayList<Skill>();

        for (Skill skill : skills)
        {
            if (skill == null)
                continue;

            if (!skill.isActive())
                continue;

            if (skill.getName() == null || skill.getName().isEmpty())
                continue;

            validSkills.add(skill);
        }

        final int pageSize = 50;
        int start = page * pageSize;
        int end = Math.min(start + pageSize, validSkills.size());

        int count = 0;

        for (int i = start; i < end; i++)
        {
            Skill skill = validSkills.get(i);

            html.append("<table width=600 border=0>");
            html.append("<tr>");

            html.append("<td width=400 align=left>");
            html.append("<font color=\"FFFFFF\">");
            html.append(skill.getName());
            html.append("</font>");
            html.append("</td>");

            html.append("<td width=100 align=center>");
            html.append("<font color=\"AAAAAA\">");
            html.append("Lv.");
            html.append(skill.getLevel());
            html.append("</font>");
            html.append("</td>");

            html.append("<td width=100 align=right>");
            html.append("<button value=\"Usar\" ");
            html.append("action=\"bypass _bbsscripts;l2f.gameserver.autofarm.AutoFarmCommunity:select ");
            html.append(slot);
            html.append(" ");
            html.append(skill.getId());
            html.append("\" width=80 height=25>");
            html.append("</td>");

            html.append("</tr>");
            html.append("</table>");

            html.append("<br>");

            count++;
        }

        if (count == 0)
        {
            html.append("<center>");
            html.append("<font color=\"FF0000\">");

            if (page == 0)
                html.append("Nenhuma skill ativa encontrada.");
            else
                html.append("Não existem mais skills nesta página.");

            html.append("</font>");
            html.append("</center>");
        }

        html.append("<br>");

        /*
         * PAGINAÇÃO
         */
        html.append("<center>");

        if (page > 0)
        {
            html.append("<button value=\"LISTA 1\" ");
            html.append("action=\"bypass _bbsscripts;l2f.gameserver.autofarm.AutoFarmCommunity:");

            if (slot == 1)
                html.append("skill1");
            else if (slot == 2)
                html.append("skill2");
            else
                html.append("skill3");

            html.append("\" width=100 height=25>");
        }

        if (page == 0 && validSkills.size() > pageSize)
        {
            html.append("<button value=\"LISTA 2\" ");
            html.append("action=\"bypass _bbsscripts;l2f.gameserver.autofarm.AutoFarmCommunity:");

            if (slot == 1)
                html.append("skill1page2");
            else if (slot == 2)
                html.append("skill2page2");
            else
                html.append("skill3page2");

            html.append("\" width=100 height=25>");
        }

        html.append("</center>");

        html.append("<br>");

        html.append("<center>");
        html.append("<button value=\"VOLTAR\" ");
        html.append("action=\"bypass _bbsscripts;l2f.gameserver.autofarm.AutoFarmCommunity:back\" ");
        html.append("width=100 height=25>");
        html.append("</center>");

        html.append("</body></html>");

        showHtml(html.toString());
    }

    public void select(String[] args)
    {
        if (self == null)
            return;

        if (args == null || args.length < 2)
            return;

        try
        {
            int slot = Integer.parseInt(args[0]);
            int skillId = Integer.parseInt(args[1]);

            if (slot < 1 || slot > 3)
                return;

            /*
             * 0 = SEM SKILL
             */
            if (skillId == 0)
            {
                int skill1 = self.getAutoFarmSkill1();
                int skill2 = self.getAutoFarmSkill2();
                int skill3 = self.getAutoFarmSkill3();

                if (slot == 1)
                    skill1 = 0;
                else if (slot == 2)
                    skill2 = 0;
                else
                    skill3 = 0;

                self.setAutoFarmSkills(skill1, skill2, skill3);

                self.sendMessage("Skill " + slot + " removida.");
                back();
                return;
            }

            Skill skill = self.getKnownSkill(skillId);

            if (skill == null)
            {
                self.sendMessage("Essa skill não pertence ao seu personagem.");
                return;
            }

            int skill1 = self.getAutoFarmSkill1();
            int skill2 = self.getAutoFarmSkill2();
            int skill3 = self.getAutoFarmSkill3();

            if (slot == 1)
                skill1 = skillId;
            else if (slot == 2)
                skill2 = skillId;
            else
                skill3 = skillId;

            self.setAutoFarmSkills(skill1, skill2, skill3);

            self.sendMessage(
                "Skill " + slot + " definida: " + skill.getName());

            back();
        }
        catch (NumberFormatException e)
        {
            self.sendMessage("Skill inválida.");
        }
    }

    public void radius()
    {
        if (self == null)
            return;

        StringBuilder html = new StringBuilder();

        html.append("<html noscrollbar>");
        html.append("<title>Auto Farm - Raio</title>");
        html.append("<body><br>");

        html.append("<center>");
        html.append("<font name=\"hs12\" color=\"LEVEL\">");
        html.append("SELECIONE O RAIO");
        html.append("</font>");
        html.append("</center>");

        html.append("<br><br>");

        int[] radii =
        {
            500,
            750,
            1000,
            1500,
            2000
        };

        for (int radius : radii)
        {
            html.append("<center>");

            html.append("<button value=\"");
            html.append(radius);
            html.append("\" action=\"bypass _bbsscripts;l2f.gameserver.autofarm.AutoFarmCommunity:setRadius ");
            html.append(radius);
            html.append("\" width=150 height=30>");

            html.append("</center>");
            html.append("<br>");
        }

        html.append("<center>");
        html.append("<button value=\"VOLTAR\" ");
        html.append("action=\"bypass _bbsscripts;l2f.gameserver.autofarm.AutoFarmCommunity:back\" ");
        html.append("width=100 height=25>");
        html.append("</center>");

        html.append("</body></html>");

        showHtml(html.toString());
    }

    public void setRadius(String[] args)
    {
        if (self == null)
            return;

        if (args == null || args.length < 1)
            return;

        try
        {
            int radius = Integer.parseInt(args[0]);

            if (radius != 500 &&
                radius != 750 &&
                radius != 1000 &&
                radius != 1500 &&
                radius != 2000)
            {
                self.sendMessage("Raio inválido.");
                return;
            }

            self.setAutoFarmRadius(radius);

            self.sendMessage(
                "Raio do Auto Farm definido para " + radius + ".");

            back();
        }
        catch (NumberFormatException e)
        {
            self.sendMessage("Raio inválido.");
        }
    }

    public void back()
    {
        if (self == null)
            return;

        String html = HtmCache.getInstance().getNotNull(
            "scripts/services/communityPVP/pages/AutoFarm.htm",
            self);

        html = html.replace(
            "%skill1%",
            getSkillName(self.getAutoFarmSkill1()));

        html = html.replace(
            "%skill2%",
            getSkillName(self.getAutoFarmSkill2()));

        html = html.replace(
            "%skill3%",
            getSkillName(self.getAutoFarmSkill3()));

        html = html.replace(
            "%radius%",
            String.valueOf(self.getAutoFarmRadius()));

        html = html.replace(
            "%status%",
            self.isAutoFarm() ? "ATIVADO" : "DESATIVADO");

        ShowBoard.separateAndSend(html, self);
    }

    private String getSkillName(int skillId)
    {
        if (skillId <= 0)
            return "Sem Skill";

        if (self == null)
            return "Sem Skill";

        Skill skill = self.getKnownSkill(skillId);

        if (skill == null)
            return "Sem Skill";

        return skill.getName();
    }
}
