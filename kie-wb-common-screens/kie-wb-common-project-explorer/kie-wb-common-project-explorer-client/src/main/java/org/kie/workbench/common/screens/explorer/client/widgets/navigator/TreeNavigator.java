package org.kie.workbench.common.screens.explorer.client.widgets.navigator;

import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.Composite;
import org.kie.workbench.common.screens.explorer.client.widgets.ViewPresenter;
import org.kie.workbench.common.screens.explorer.model.FolderItem;
import org.kie.workbench.common.screens.explorer.model.FolderItemType;
import org.kie.workbench.common.screens.explorer.model.FolderListing;
import org.uberfire.backend.vfs.FileSystem;
import org.uberfire.backend.vfs.Path;
import org.uberfire.client.resources.NavigatorResources;
import org.uberfire.client.tree.Tree;
import org.uberfire.client.tree.TreeItem;
import org.uberfire.workbench.type.DotResourceTypeDefinition;

import static org.uberfire.commons.validation.PortablePreconditions.*;

@Dependent
public class TreeNavigator extends Composite implements Navigator {

    private static final String LAZY_LOAD = "Loading...";

    @Inject
    private DotResourceTypeDefinition hiddenTypeDef;

    private NavigatorOptions options = new NavigatorOptions();

    private final Tree tree = new Tree() {{
        addStyleName( NavigatorResources.INSTANCE.css().treeNav() );
    }};
    private ViewPresenter presenter;

    private FolderListing activeContent;

    @PostConstruct
    public void init() {
        initWidget( tree );

        tree.addOpenHandler( new OpenHandler<TreeItem>() {
            @Override
            public void onOpen( final OpenEvent<TreeItem> event ) {
                if ( needsLoading( event.getTarget() ) ) {
                    presenter.loadContent( (FolderItem) event.getTarget().getUserObject(), null );
                }
            }
        } );

        tree.addSelectionHandler( new SelectionHandler<TreeItem>() {
            @Override
            public void onSelection( SelectionEvent<TreeItem> event ) {
                if ( event.getSelectedItem().getUserObject() != null && event.getSelectedItem().getUserObject() instanceof FolderItem ) {
                    presenter.itemSelected( (FolderItem) event.getSelectedItem().getUserObject() );
                }
            }
        } );
    }

    @Override
    public void setOptions( final NavigatorOptions options ) {
        this.options = options;
    }

    @Override
    public void loadContent( final FolderListing content ) {
        if ( content == null || content.getItem() == null ) {
            clear();
            return;
        }
        if ( content.equals( activeContent ) ) {
            tree.getSelectedItem().setState( TreeItem.State.OPEN, true, false );
            return;
        }

        activeContent = content;

        TreeItem item = null;
        if ( !tree.isEmpty() ) {
            item = findItemInTree( content.getItem() );
        }

        if ( item == null ) {
            if ( content.getSegments().isEmpty() ) {
                final FolderItem rootItem = content.getItem();
                item = new TreeItem( TreeItem.Type.FOLDER, rootItem.getFileName() );
                tree.addItem( item );
                item.setUserObject( rootItem );
            } else {
                final TreeItem parent = loadRoots( content.getSegments() );
                item = parent.addItem( TreeItem.Type.FOLDER, content.getItem().getFileName() );
                item.setUserObject( content.getItem() );
            }
        }

        item.setState( TreeItem.State.OPEN, true, false );
        tree.setSelectedItem( item );

        loadContent( new TreeNavigatorItemImpl( item ), content );
    }

    private TreeItem findItemInTree( final FolderItem xxx ) {
        TreeItem item = null;
        for ( final TreeItem treeItem : tree.getItems() ) {
            if ( item != null ) {
                return item;
            }
            if ( xxx != null && xxx.equals( treeItem.getUserObject() ) ) {
                item = treeItem;
            } else {
                item = findItemInChildren( treeItem, xxx );
            }
        }
        return item;
    }

    private TreeItem loadRoots( final List<FolderItem> segments ) {
        TreeItem parent = null;
        for ( final FolderItem segment : segments ) {
            TreeItem item;
            if ( tree.isEmpty() ) {
                item = new TreeItem( TreeItem.Type.FOLDER, segment.getFileName() );
                item.setUserObject( segment );
                tree.addItem( item );
                parent = item;
            } else {
                if ( parent == null ) {
                    parent = findItemInTree( segment );
                } else {
                    item = findItemInTree( segment );
                    if ( item == null ) {
                        item = parent.addItem( TreeItem.Type.FOLDER, segment.getFileName() );
                        item.setUserObject( segment );
                    } else if ( needsLoading( item ) ) {
                        item.getChild( 0 ).getElement().getStyle().setDisplay( Style.Display.NONE );
                    }
                    parent = item;
                }
            }
        }
        return parent;
    }

    private TreeItem findItemInChildren( final TreeItem item,
                                         final FolderItem target ) {
        TreeItem result = null;
        for ( final TreeItem treeItem : item.getChildren() ) {
            if ( result != null ) {
                break;
            }
            if ( target != null && target.equals( treeItem.getUserObject() ) ) {
                result = treeItem;
            } else {
                result = findItemInChildren( treeItem, target );
            }
        }

        return result;
    }

    @Override
    public void clear() {
        tree.clear();
        activeContent = null;
    }

    @Override
    public void setPresenter( final ViewPresenter presenter ) {
        this.presenter = presenter;
    }

    private void loadContent( final NavigatorItem parent,
                              final FolderListing content ) {
        if ( content != null ) {
            for ( final FolderItem folderItem : content.getContent() ) {
                if ( folderItem.getType().equals( FolderItemType.FOLDER ) ) {
                    if ( options.showDirectories() ) {
                        parent.addDirectory( folderItem );
                    }
                } else {
                    if ( options.showFiles() ) {
                        if ( !options.showHiddenFiles() && !isHidden( folderItem ) ) {
                            parent.addFile( folderItem );
                        } else {
                            parent.addFile( folderItem );
                        }
                    }
                }
            }
            parent.cleanup();
        }
    }

    private boolean isHidden( final FolderItem folderItem ) {
        return hiddenTypeDef.accept( new Path() {
            @Override
            public FileSystem getFileSystem() {
                return null;
            }

            @Override
            public String getFileName() {
                return folderItem.getFileName();
            }

            @Override
            public String toURI() {
                return null;
            }

            @Override
            public int compareTo( final Path path ) {
                return 0;
            }
        } );
    }

    private boolean needsLoading( final TreeItem item ) {
        return item.getChildCount() == 1 && LAZY_LOAD.equals( item.getChild( 0 ).getText() );
    }

    private class TreeNavigatorItemImpl implements NavigatorItem {

        private final TreeItem parent;

        TreeNavigatorItemImpl( final TreeItem treeItem ) {
            this.parent = checkNotNull( "parent", treeItem );
        }

        public void addDirectory( final FolderItem child ) {
            checkCleanupLoading();

            for ( final TreeItem treeItem : parent.getChildren() ) {
                if ( treeItem.getUserObject() != null && treeItem.getUserObject().equals( child ) ) {
                    return;
                }
            }

            final TreeItem newDirectory = parent.addItem( TreeItem.Type.FOLDER, child.getFileName() );
            newDirectory.addItem( TreeItem.Type.LOADING, LAZY_LOAD );
            newDirectory.setUserObject( child );
        }

        public void addFile( final FolderItem child ) {
            checkCleanupLoading();

            for ( final TreeItem treeItem : parent.getChildren() ) {
                if ( treeItem.getUserObject() != null && treeItem.getUserObject().equals( child ) ) {
                    return;
                }
            }

            final TreeItem newFile = parent.addItem( TreeItem.Type.ITEM, child.getFileName() );
            newFile.setUserObject( child );
        }

        @Override
        public void cleanup() {
            checkCleanupLoading();
        }

        public void checkCleanupLoading() {
            if ( parent.getChild( 0 ) != null && parent.getChild( 0 ).getUserObject() == null ) {
                parent.getChild( 0 ).remove();
            }
        }

    }
}