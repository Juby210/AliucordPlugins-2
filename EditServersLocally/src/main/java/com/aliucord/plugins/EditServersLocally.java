package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.widget.NestedScrollView;

import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.patcher.PreHook;
import com.aliucord.plugins.DataClasses.ChannelData;
import com.aliucord.plugins.DataClasses.GuildData;
import com.aliucord.utils.DimenUtils;
import com.discord.api.guild.Guild;
import com.discord.databinding.WidgetChannelsListItemChannelBinding;
import com.discord.databinding.WidgetGuildContextMenuBinding;
import com.discord.stores.StoreStream;
import com.discord.utilities.icon.IconUtils;
import com.discord.widgets.channels.list.WidgetChannelsListAdapter;
import com.discord.widgets.channels.list.items.ChannelListItem;
import com.discord.widgets.channels.list.items.ChannelListItemTextChannel;
import com.discord.widgets.guilds.contextmenu.GuildContextMenuViewModel;
import com.discord.widgets.guilds.contextmenu.WidgetGuildContextMenu;
import com.google.gson.reflect.TypeToken;
import com.lytefast.flexinput.R;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.utils.ReflectUtils;
import com.aliucord.wrappers.ChannelWrapper;
import com.discord.api.channel.Channel;
import com.discord.databinding.WidgetChannelsListItemActionsBinding;
import com.discord.widgets.channels.list.WidgetChannelsListItemChannelActions;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@AliucordPlugin
public class EditServersLocally extends Plugin {

   // ArrayList<ChannelData> dataList =settings.getObject("data",new ArrayList<>(), TypeToken.getParameterized(ArrayList.class, ChannelData.class).getType());
    AtomicReference<HashMap<Long, View>> channels = new AtomicReference<>(new HashMap<>());
    AtomicLong currentGuild= new AtomicLong();
    Logger logger = new Logger("EditServersLocally");
    public HashMap<Long, ChannelData> channelData = settings.getObject("channelData",new HashMap<Long, ChannelData>(),TypeToken.getParameterized(HashMap.class, Long.class,ChannelData.class).getType());
    public HashMap<Long,GuildData> guildData = settings.getObject("guildData",new HashMap<Long, GuildData>(),TypeToken.getParameterized(HashMap.class, Long.class,GuildData.class).getType());




    Context context;
    @SuppressLint("ResourceType")
    @Override
    public void start(Context context) throws Throwable {

        this.context= context;

        settingsTab = new SettingsTab(BottomSheet.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(settings);

        /*
        patcher.patch(Channel.class.getDeclaredMethod("m"),new Hook((cf)->{
            //patching 'getChannelName' method so I can change channels name
            Channel ch = (Channel) cf.thisObject;
            ChannelData data =getChannelData(ChannelWrapper.getId(ch));
            if(data.channelName!=null){cf.setResult(data.channelName);}
        }));

         */



        patcher.patch(Guild.class.getDeclaredMethod("v"),new PreHook((cf)->{
            var thisobj =(Channel) cf.thisObject;
            try {
                long guildid = (long) ReflectUtils.getField(thisobj,"f1573id");
                GuildData data = guildData.get(guildid);

                logger.info(String.valueOf(guildid));
                if (data.serverName!=null){
                    cf.setResult(data.serverName);
                }

            } catch (NoSuchFieldException | IllegalAccessException e) {
                logger.error(e);
            }
        }));


        patcher.patch(com.discord.models.guild.Guild.class.getDeclaredMethod("getName"),new PreHook((cf)->{
            com.discord.models.guild.Guild guild = (com.discord.models.guild.Guild) cf.thisObject;
            GuildData data = guildData.get(guild.getId());
            if(data.serverName!=null){
                cf.setResult(data.serverName);
            }

        }));
        for (Constructor<?> constructor : com.discord.models.guild.Guild.class.getConstructors()) {
            patcher.patch(constructor,new Hook((cf)->{
                try {
                    com.discord.models.guild.Guild guild = (com.discord.models.guild.Guild) cf.thisObject;

                    GuildData data = getGuildData(guild.getId());
                    if(data.serverName!=null){
                        ReflectUtils.setField(cf.thisObject,"name",data.serverName);
                    }

                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }

            }));
        }



        patcher.patch(WidgetGuildContextMenu.class.getDeclaredMethod("configureUI", GuildContextMenuViewModel.ViewState.class)
                ,new Hook((cf)->{
            //adding set server name,photo to Guild Settings
            var thisObject = (WidgetGuildContextMenu)cf.thisObject;

            try {
                var state = (GuildContextMenuViewModel.ViewState.Valid) cf.args[0];
                Method method = ReflectUtils.getMethodByArgs(WidgetGuildContextMenu.class,"getBinding");
                WidgetGuildContextMenuBinding binding = (WidgetGuildContextMenuBinding) method.invoke(thisObject);
                LinearLayout v = (LinearLayout) binding.e.getParent();
                var guild =state.getGuild();

                TextView tw = new TextView(v.getContext(),null,0,R.h.UiKit_Settings_Item_Icon);
                tw.setLayoutParams(binding.e.getLayoutParams());
                Context ctx = binding.e.getContext();
                tw.setText("Local Server Settings");
                tw.setOnClickListener(v1 -> {
                    ServerSettingsFragment page = new ServerSettingsFragment(guild,EditServersLocally.this);
                    Utils.openPageWithProxy(ctx, page);
                    v.removeView(tw);

                });
                v.addView(tw);

            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }

        }));

        patcher.patch(WidgetChannelsListAdapter.ItemChannelText.class.getDeclaredMethod("onConfigure", int.class, ChannelListItem.class),new Hook(
                (cf)->{
            WidgetChannelsListAdapter.ItemChannelText thisobj = (WidgetChannelsListAdapter.ItemChannelText) cf.thisObject;
            try {

                WidgetChannelsListItemChannelBinding binding = (WidgetChannelsListItemChannelBinding) ReflectUtils.getField(thisobj,"binding");
                ChannelListItemTextChannel channelListItemTextChannel  = (ChannelListItemTextChannel) cf.args[1];
                ChannelWrapper ch = new ChannelWrapper(channelListItemTextChannel.getChannel());
                if (ch.getGuildId()!= currentGuild.get()){
                    currentGuild.set(ch.getGuildId());
                    channels.set(new HashMap<>());
                }
                channels.get().put(ch.getId(),binding.d);

                //getting saved names and changing channel name to it
                long i =ChannelWrapper.getId(channelListItemTextChannel.component1());

                if (channelData.containsKey(i)){
                    var chdata =getChannelData(i);
                    binding.d.setText(chdata.channelName);
                    Channel cha =channelListItemTextChannel.component1();

                    chdata.orginalName=ChannelWrapper.getName(cha);
                    channelData.put(chdata.channelID,chdata);
                    setChannelData();
                    ReflectUtils.setField(cha,"name",chdata.channelName);
                    logger.info(channelData.toString());
                }

            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }));
        /*
        patcher.patch(GuildListViewHolder.GuildViewHolder.class.getDeclaredMethod("configureGuildIconImage", com.discord.models.guild.Guild.class, boolean.class),
                new Hook((cf)->{
                    var thisobj = (GuildListViewHolder.GuildViewHolder)cf.thisObject;
                    var guild = (com.discord.models.guild.Guild)cf.args[0];
                    try {
                        WidgetGuildsListItemGuildBinding binding = (WidgetGuildsListItemGuildBinding) ReflectUtils.getField(thisobj,"bindingGuild");
                        GuildData data = getGuildData(guild.getId());
                        if (data.imageURL!=null){

                            binding.d.setImageURI(data.imageURL);
                            logger.info(IconUtils.getForGuild(guild));

                            //ReflectUtils.setField(guild,"icon","changed");
                            //StoreStream.access$handleGuildUpdate(StoreStream.getNotices().getStream(),GuildUtilsKt.createApiGuild(guild));
                        }


                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }));

         */

        patcher.patch(IconUtils.class.getDeclaredMethod("getForGuild", Long.class, String.class, String.class, boolean.class, Integer.class),
                new PreHook((cf)->{
                    long guildID = (long) cf.args[0];

                    GuildData data = getGuildData(guildID);
                    if(data.imageURL!=null){
                        cf.setResult(data.imageURL);
                    }
                }));


        patcher.patch(WidgetChannelsListItemChannelActions.class.getDeclaredMethod("configureUI", WidgetChannelsListItemChannelActions.Model.class),
                new Hook((cf)->{
                    //Putting Set ChannelName button to actions
                    WidgetChannelsListItemChannelActions.Model model = (WidgetChannelsListItemChannelActions.Model) cf.args[0];
                    try {
                        WidgetChannelsListItemChannelActions actions = (WidgetChannelsListItemChannelActions) cf.thisObject;

                        var a = (NestedScrollView)actions.requireView();
                        var layout = (LinearLayout)a.getChildAt(0);

                        Method method = ReflectUtils.getMethodByArgs(cf.thisObject.getClass(),"getBinding");
                        WidgetChannelsListItemActionsBinding binding = (WidgetChannelsListItemActionsBinding) method.invoke(cf.thisObject);
                        View v =  binding.j;

                        ViewGroup.LayoutParams param = a.getLayoutParams();
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(param.width,param.height);
                        params.leftMargin = DimenUtils.dpToPx(20);


                        TextView tw = new TextView(v.getContext(),null,0,R.h.UiKit_Settings_Item_Icon);
                        tw.setText("Set Channel Name");
                        tw.setLayoutParams(v.getLayoutParams());

                        tw.setId(View.generateViewId());
                        tw.setOnClickListener(v1 -> {

                            EditText et =new EditText(v.getContext());
                            et.setSelectAllOnFocus(true);
                            LinearLayout lay = new LinearLayout(v.getContext());
                            lay.addView(et);
                            et.setLayoutParams(params);

                            long chid = ChannelWrapper.getId(model.getChannel());
                            if (channelData.containsKey(chid)){
                                et.setText(getChannelData(chid).channelName);
                            }

                            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                            builder.setMessage("Set Channel Name")
                                    .setPositiveButton("Set", (dialog, id) -> {
                                        addData(new ChannelData(model.getGuild().getId(),ChannelWrapper.getId(model.getChannel()),et.getText().toString()));

                                    })
                                    .setNegativeButton("Cancel", (dialog, id) -> {}).setView(lay).setNeutralButton("Remove",(dialog, which) -> removeData(ChannelWrapper.getId(model.getChannel())));

                            builder.create().show();

                        });
                        layout.addView(tw);

                    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                        logger.error(e);
                    }

                }));
    }
    public ChannelData getChannelData(long id){ return channelData.get(id)!=null?channelData.get(id):new ChannelData(id); }
    public GuildData getGuildData(long id){ return guildData.get(id)!=null?guildData.get(id):new GuildData(id); }
    public void setGuildData() { settings.setObject("guildData",guildData); }
    public void updateData(){
        channelData = settings.getObject("channelData",new HashMap<Long, ChannelData>(),TypeToken.getParameterized(HashMap.class, Long.class,ChannelData.class).getType());
        guildData = settings.getObject("guildData",new HashMap<Long, GuildData>(),TypeToken.getParameterized(HashMap.class, Long.class,GuildData.class).getType());
    }

    public Channel getModifiedChannel(long id){
        //gets Channel,replaces its name and returns it
        Channel ch = StoreStream.getChannels().getChannel(id);
        ChannelData data = getChannelData(id);
        if(data.channelName!=null){
            try {
                ReflectUtils.setField(ch,"name",data.channelName);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
            return ch;
        }
        return null;
    }

    public void addData(ChannelData data){
        channelData.put(data.channelID,data);
        updateChannel(data.channelID,data.channelName);
        setChannelData();
    }

    public void removeData(long channelID){

        updateChannel(channelID,"");
        channelData.remove(channelID);
        setChannelData();

    }
    public void updateChannel(long channelID,String chname)  {
        ChannelData data = getChannelData(channelID);
        try{
            logger.info(data.orginalName);
            if (chname.isEmpty()) chname=data.orginalName;
            TextView v = (TextView) channels.get().get(channelID);
            if (!chname.isEmpty()){
                v.setText(chname);
            } else{
                v.setText(ChannelWrapper.getName(StoreStream.getChannels().getChannel(channelID)));
            }

        }catch (Exception e){logger.error(e);}

        Channel ch = StoreStream.getChannels().getChannel(channelID);


        try {ReflectUtils.setField(ch,"name",chname); } catch (NoSuchFieldException | IllegalAccessException e) { e.printStackTrace(); }
        StoreStream.getChannels().handleChannelOrThreadCreateOrUpdate(ch);
    }

    public void setChannelData(){ settings.setObject("channelData",channelData); }



    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }
}
