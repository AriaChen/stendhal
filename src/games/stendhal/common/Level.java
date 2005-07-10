package games.stendhal.common;

import java.util.*;
import marauroa.common.*;

public class Level
  {
  static private int LEVELS=100;

  static Vector<Integer> table;
  static
    {
    table=new Vector<Integer>();
    table.add(0);
    table.add(50);
    table.add(100);
    table.add(200);
    table.add(400);
    table.add(800);

    for(int i=5;i<LEVELS;i++)
      {
      int exp=((i*16+i*i*5+i*i*i*10+300)/100)*100;
      table.add(exp);
      }

    for(int i=0;i<LEVELS;i++)
      {
      Logger.trace("Level::(static)","D","Level "+i+": "+table.get(i));
      }
    }

  public static int maxLevel()
    {
      return LEVELS - 1;
    }

  public static int getLevel(int exp)
    {
    int first = 0;
    int last = LEVELS - 1;
    if(exp <= table.get(first)) return first;
    if(exp >= table.get(last)) return last;
    while(last - first > 1)
      {
      int current = first + ((last - first) / 2);
      if(exp < table.get(current))
        {
        last = current;
        }
      else
        {
        first = current;
        }
      }
    return first;
    }

  public static int getXP(int level)
    {
      if(level >= 0 && level < table.size())
        {
        return table.get(level);
        }
      return -1;
    }

  public static int changeLevel(int exp, int added)
    {
    int i;
    for(i=0;i<LEVELS;i++)
      {
      if(exp<table.get(i))
        {
        break;
        }
      }

    for(int j=i;j<LEVELS;j++)
      {
      if(exp+added<table.get(j))
        {
        return j-i;
        }
      }

    return 0;
    }
  }

