/*
    Copyright 2008-2011 Josh Drummond

    This file is part of WebPasswordSafe.

    WebPasswordSafe is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    WebPasswordSafe is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with WebPasswordSafe; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
package net.webpasswordsafe.client.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.webpasswordsafe.client.WebPasswordSafe;
import net.webpasswordsafe.client.i18n.TextConstants;
import net.webpasswordsafe.client.i18n.TextMessages;
import net.webpasswordsafe.client.remote.PasswordService;
import net.webpasswordsafe.common.model.Password;
import net.webpasswordsafe.common.model.Tag;
import net.webpasswordsafe.common.util.Constants;
import net.webpasswordsafe.common.util.Utils;
import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.data.BaseModel;
import com.extjs.gxt.ui.client.data.BaseTreeModel;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.event.KeyListener;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.store.TreeStore;
import com.extjs.gxt.ui.client.util.Format;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.util.Padding;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.CheckBox;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FillLayout;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayoutData;
import com.extjs.gxt.ui.client.widget.layout.BoxLayout.BoxLayoutPack;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout.HBoxLayoutAlign;
import com.extjs.gxt.ui.client.widget.treepanel.TreePanel;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.extjs.gxt.ui.client.widget.treepanel.TreePanel.CheckNodes;
import com.extjs.gxt.ui.client.widget.treepanel.TreePanel.CheckCascade;


/**
 * @author Josh Drummond
 *
 */
public class PasswordSearchPanel extends ContentPanel implements TagLoadListener
{
    private final static TextConstants textConstants = GWT.create(TextConstants.class);
    private final static TextMessages textMessages = GWT.create(TextMessages.class);
    private Grid<PasswordSearchData> passwordGrid;
    private ListStore<PasswordSearchData> gridStore;
    private TreeStore<TagData> treeStore;
    private TreePanel<TagData> tagTree;
    private TextField<String> searchTextBox;
    private CheckBox activeOnlyCheckBox;
    //private static final String TOOLTIP_VIEW_PASSWORD_VALUE = "Click to view current password value.";
    //private static final String TOOLTIP_EDIT_PASSWORD = "Click to edit password and/or permissions.";

    public PasswordSearchPanel()
    {
        setLayout(new BorderLayout());
        setHeaderVisible(false);
        
        ContentPanel northPanel = new ContentPanel();
        northPanel.setHeading(textConstants.passwordSearch());
        ContentPanel westPanel = new ContentPanel(new FillLayout());
        westPanel.setHeading(textConstants.tags());
        ContentPanel centerPanel = new ContentPanel(new FillLayout());
        centerPanel.setHeading(textConstants.passwords());
        
        HBoxLayout northLayout = new HBoxLayout();  
        northLayout.setPadding(new Padding(5));  
        northLayout.setHBoxLayoutAlign(HBoxLayoutAlign.MIDDLE);  
        northLayout.setPack(BoxLayoutPack.CENTER);  
        northPanel.setLayout(northLayout);  

        searchTextBox = new TextField<String>();
        searchTextBox.addKeyListener(new KeyListener() {
            @Override
            public void componentKeyPress(ComponentEvent event) {
                if (event.getKeyCode() == KeyCodes.KEY_ENTER)
                {
                    doSearch();
                }
            }
        });
        searchTextBox.setWidth(300);
        searchTextBox.setMaxLength(1000);
        searchTextBox.focus();

        Button searchButton = new Button(textConstants.search(), new SelectionListener<ButtonEvent>() {
            @Override
            public void componentSelected(ButtonEvent ce) {
                doSearch();
            }
        });
        activeOnlyCheckBox = new CheckBox();
        activeOnlyCheckBox.setBoxLabel(textConstants.activeOnly());
        activeOnlyCheckBox.setValue(true);
        northPanel.add(searchTextBox, new HBoxLayoutData(new Margins(0, 5, 0, 0)));  
        northPanel.add(searchButton, new HBoxLayoutData(new Margins(0, 5, 0, 0)));  
        northPanel.add(activeOnlyCheckBox, new HBoxLayoutData(new Margins(0, 5, 0, 5)));  
        
        treeStore = new TreeStore<TagData>();
        tagTree = new TreePanel<TagData>(treeStore);
        tagTree.setCheckStyle(CheckCascade.NONE);
        tagTree.setCheckNodes(CheckNodes.LEAF);
        tagTree.setCheckable(true);
        tagTree.setDisplayProperty(Constants.NAME);
        tagTree.setWidth(250);  
        westPanel.add(tagTree);
        
        centerPanel.setScrollMode(Scroll.AUTOX);
        List<ColumnConfig> configs = new ArrayList<ColumnConfig>(4);
        ColumnConfig column = new ColumnConfig();
        //column.setToolTip(TOOLTIP_EDIT_PASSWORD);
        column.setId(Constants.TITLE);
        column.setHeader(textConstants.title());
        column.setWidth(200);
        configs.add(column);
        column = new ColumnConfig();
        column.setId(Constants.USERNAME);
        column.setHeader(textConstants.username());
        column.setWidth(100);
        configs.add(column);
        column = new ColumnConfig();
        //column.setToolTip(TOOLTIP_VIEW_PASSWORD_VALUE);
        column.setId(Constants.PASSWORD);
        column.setHeader(textConstants.password());
        column.setWidth(100);
        configs.add(column);
        column = new ColumnConfig();
        column.setId(Constants.TAGS);
        column.setHeader(textConstants.tags());
        column.setWidth(200);
        configs.add(column);
        column = new ColumnConfig();
        column.setId(Constants.NOTES);
        column.setHeader(textConstants.notes());
        column.setWidth(300);
        configs.add(column);
        
        gridStore = new ListStore<PasswordSearchData>();
        ColumnModel cm = new ColumnModel(configs);
        passwordGrid = new Grid<PasswordSearchData>(gridStore, cm);
        passwordGrid.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        passwordGrid.setStyleAttribute("borderTop", "none");
        passwordGrid.setBorders(true);
        passwordGrid.setStripeRows(true);
        passwordGrid.addListener(Events.CellDoubleClick, new Listener<GridEvent<PasswordSearchData>>()
        {
            @Override
            public void handleEvent(GridEvent<PasswordSearchData> ge)
            {
                if (2 == ge.getColIndex())
                {
                    doShowPasswordPopup((Long)ge.getModel().get(Constants.ID));
                }
                else
                {
                    doLoadPasswordDialog((Long)ge.getModel().get(Constants.ID));
                }
            }
        });
        centerPanel.add(passwordGrid);
        
        BorderLayoutData northData = new BorderLayoutData(LayoutRegion.NORTH, 100);  
        northData.setCollapsible(true);  
        northData.setFloatable(false);  
        northData.setHideCollapseTool(false);  
        northData.setSplit(true);
        northData.setMargins(new Margins(5, 5, 0, 5));  
        
        BorderLayoutData westData = new BorderLayoutData(LayoutRegion.WEST, 150);  
        westData.setSplit(true);  
        westData.setCollapsible(true);  
        westData.setMargins(new Margins(5));  
        
        BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);  
        centerData.setMargins(new Margins(5, 0, 5, 0));  
        
        add(northPanel, northData);
        add(westPanel, westData);
        add(centerPanel, centerData);
        
        doLoadTags();
    }

    public void openSelectedPassword()
    {
        PasswordSearchData p = passwordGrid.getSelectionModel().getSelectedItem();
        if (null != p)
        {
            doLoadPasswordDialog((Long)p.get(Constants.ID));
        }
    }
    
    public void getSelectedCurrentPasswordData()
    {
        PasswordSearchData p = passwordGrid.getSelectionModel().getSelectedItem();
        if (null != p)
        {
            doShowPasswordPopup((Long)p.get(Constants.ID));
        }
    }
    
    private void doLoadTags()
    {
        AsyncCallback<List<Tag>> callback = new AsyncCallback<List<Tag>>()
        {
            @Override
            public void onFailure(Throwable caught)
            {
                WebPasswordSafe.handleServerFailure(caught);
            }
            @Override
            public void onSuccess(List<Tag> result)
            {
                refreshTags(result);
            }
        };
        PasswordService.Util.getInstance().getAvailableTags(callback);
    }
    
    private void doLoadPasswordDialog(long passwordId)
    {
        AsyncCallback<Password> callback = new AsyncCallback<Password>()
        {
            @Override
            public void onFailure(Throwable caught)
            {
                WebPasswordSafe.handleServerFailure(caught);
            }
            @Override
            public void onSuccess(Password result)
            {
                if (null != result)
                {
                    openPasswordDialog(result);
                }
                else
                {
                    MessageBox.alert(textConstants.error(), textMessages.noAccessPasswordRead(), null);
                }
            }
        };
        PasswordService.Util.getInstance().getPassword(passwordId, callback);
    }
    
    private void openPasswordDialog(Password password)
    {
        new PasswordDialog(password, this).show();
    }
    
    private void refreshTags(List<Tag> tags)
    {
        treeStore.removeAll();
        for (Tag tag : tags)
        {
            treeStore.add(new TagData(tag), true);
        }
    }

    private void doSearch()
    {
        List<TagData> selectedTagData = tagTree.getCheckedSelection();
        Set<Tag> selectedTags = new HashSet<Tag>(selectedTagData.size());
        for (TagData selectedTag : selectedTagData)
        {
            selectedTags.add((Tag)selectedTag.get(Constants.TAG));
        }
        AsyncCallback<List<Password>> callback = new AsyncCallback<List<Password>>()
        {
            @Override
            public void onFailure(Throwable caught)
            {
                WebPasswordSafe.handleServerFailure(caught);
            }
            @Override
            public void onSuccess(List<Password> result)
            {
                Info.display(textConstants.status(), textMessages.foundPasswords(result.size()));
                refreshTable(result);
            }
        };
        PasswordService.Util.getInstance().searchPassword(Utils.safeString(searchTextBox.getValue()), activeOnlyCheckBox.getValue(), selectedTags, callback);
    }

    private void refreshTable(List<Password> passwords)
    {
        gridStore.removeAll();
        for (Password password : passwords)
        {
            gridStore.add(new PasswordSearchData(password.getId(), password.getName(), password.getUsername(), password.getTagsAsString(), password.getNotes()));
        }
    }

    @Override
    public void reloadTags()
    {
        doLoadTags();
    }
    
    private class PasswordSearchData extends BaseModel
    {
        private static final long serialVersionUID = 1L;

        public PasswordSearchData(long id, String title, String username, String tags, String notes)
        {
            set(Constants.ID, id);
            set(Constants.TITLE, Format.htmlEncode(title));
            set(Constants.USERNAME, Format.htmlEncode(username));
            set(Constants.PASSWORD, textConstants.displayCensored());
            set(Constants.TAGS, Format.htmlEncode(tags));
            set(Constants.NOTES, Format.htmlEncode(notes));
        }
    }

    private void doShowPasswordPopup(long passwordId)
    {
        AsyncCallback<String> callback = new AsyncCallback<String>()
        {
            @Override
            public void onFailure(Throwable caught)
            {
                WebPasswordSafe.handleServerFailure(caught);
            }
            @Override
            public void onSuccess(String result)
            {
                Dialog popup = new Dialog();
                popup.setHeading(textConstants.currentPassword());
                popup.setButtons(Dialog.CLOSE);
                popup.addText(Format.htmlEncode(result));
                popup.setScrollMode(Scroll.AUTO);
                popup.setHideOnButtonClick(true);
                popup.show();
            }
        };
        PasswordService.Util.getInstance().getCurrentPassword(passwordId, callback);
    }
    
    private class TagData extends BaseTreeModel
    {
        private static final long serialVersionUID = 1L;
        
        public TagData(Tag tag)
        {
            set(Constants.ID, tag.getId());
            set(Constants.NAME, Format.htmlEncode(tag.getName()));
            set(Constants.TAG, tag);
        }
    }
}