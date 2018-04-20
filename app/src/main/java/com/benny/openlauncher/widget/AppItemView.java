package com.benny.openlauncher.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.View;

import com.benny.openlauncher.R;
import com.benny.openlauncher.activity.Home;
import com.benny.openlauncher.interfaces.IconDrawer;
import com.benny.openlauncher.interfaces.IconProvider;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.model.Item;
import com.benny.openlauncher.util.App;
import com.benny.openlauncher.util.BaseIconProvider;
import com.benny.openlauncher.util.Definitions;
import com.benny.openlauncher.util.DragAction;
import com.benny.openlauncher.util.DragNDropHandler;
import com.benny.openlauncher.util.Tool;
import com.benny.openlauncher.viewutil.DesktopCallBack;
import com.benny.openlauncher.viewutil.GroupIconDrawable;
import com.benny.openlauncher.viewutil.ItemGestureListener;

public class AppItemView extends View implements Drawable.Callback, IconDrawer {

    private static final int MIN_ICON_TEXT_MARGIN = 8;
    private static final char ELLIPSIS = '…';

    private Drawable icon = null;
    private BaseIconProvider iconProvider;
    private String label;
    private Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Rect textContainer = new Rect(), testTextContainer = new Rect();
    private Typeface typeface;
    private float iconSize;
    private boolean showLabel = true;
    private boolean vibrateWhenLongPress;
    private float labelHeight;
    private int targetedWidth;
    private int fontSizeSp;
    private int targetedHeightPadding;
    private float heightPadding;
    private boolean fastAdapterItem;

    public AppItemView(Context context) {
        this(context, null);
    }

    public AppItemView(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (typeface == null) {
            typeface = Typeface.createFromAsset(getContext().getAssets(), "RobotoCondensed-Regular.ttf");
        }

        setWillNotDraw(false);
        setDrawingCacheEnabled(true);
        setWillNotCacheDrawing(false);

        labelHeight = Tool.dp2px(14, getContext());

        textPaint.setTextSize(Tool.sp2px(getContext(), 13));
        textPaint.setColor(Color.DKGRAY);
        textPaint.setTypeface(typeface);
    }

    public static AppItemView createAppItemViewPopup(Context context, Item groupItem, App item, int iconSize, float fontSizeSp) {
        AppItemView.Builder b = new AppItemView.Builder(context, iconSize)
                .withOnTouchGetPosition(groupItem, Setup.itemGestureCallback())
                .setTextColor(Setup.appSettings().getFolderLabelColor())
                .setFontSize(context, fontSizeSp);
        if (groupItem.getType() == Item.Type.SHORTCUT) {
            b.setShortcutItem(groupItem);
        } else {
            App app = Setup.appLoader().findItemApp(groupItem);
            if (app != null) {
                b.setAppItem(groupItem, app);
            }
        }
        return b.getView();
    }

    public static View createDrawerAppItemView(Context context, final Home home, App app, int iconSize, AppItemView.LongPressCallBack longPressCallBack) {
        return new AppItemView.Builder(context, iconSize)
                .setAppItem(app)
                .withOnTouchGetPosition(null, null)
                .withOnLongClick(app, DragAction.Action.APP_DRAWER, longPressCallBack)
                .setLabelVisibility(Setup.appSettings().isDrawerShowLabel())
                .setTextColor(Setup.appSettings().getDrawerLabelColor())
                .setFontSize(context, Setup.appSettings().getDrawerLabelFontSize())
                .getView();
    }

    @Override
    public Bitmap getDrawingCache() {
        return Tool.drawableToBitmap(icon);
    }

    public View getView() {
        return this;
    }

    public IconProvider getIconProvider() {
        return iconProvider;
    }

    public void setIconProvider(BaseIconProvider iconProvider) {
        this.iconProvider = iconProvider;
        iconProvider.loadIconIntoIconDrawer(this, (int) iconSize, 0);
    }

    public Drawable getCurrentIcon() {
        return icon;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public float getIconSize() {
        return iconSize;
    }

    public void setIconSize(float iconSize) {
        this.iconSize = iconSize;
    }

    public boolean getShowLabel() {
        return showLabel;
    }

    public void setTargetedWidth(int width) {
        targetedWidth = width;
    }

    public void setTargetedHeightPadding(int padding) {
        targetedHeightPadding = padding;
    }

    public void load() {
        if (iconProvider != null) {
            iconProvider.loadIconIntoIconDrawer(this, (int) iconSize, 0);
        }
    }

    public void reset() {
        if (iconProvider != null) {
            iconProvider.cancelLoad(this);
        }
        label = "";
        icon = null;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float mWidth = iconSize;
        float mHeight = iconSize + (showLabel ? 0 : labelHeight);
        if (targetedWidth != 0) {
            mWidth = targetedWidth;
        }
        setMeasuredDimension((int) Math.ceil(mWidth), (int) Math.ceil((int) mHeight) + Tool.dp2px(2, getContext()) + targetedHeightPadding * 2);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        heightPadding = (getHeight() - iconSize - (showLabel ? labelHeight : 0)) / 2f;

        if (label != null && showLabel) {
            textPaint.getTextBounds(label, 0, label.length(), textContainer);
            int maxTextWidth = getWidth() - MIN_ICON_TEXT_MARGIN * 2;

            // use ellipsis if the label is too long
            if (textContainer.width() > maxTextWidth) {
                String testLabel = label + ELLIPSIS;
                textPaint.getTextBounds(testLabel, 0, testLabel.length(), testTextContainer);

                //Premeditate to be faster
                float characterSize = testTextContainer.width() / testLabel.length();
                int charsToTruncate = (int) ((testTextContainer.width() - maxTextWidth) / characterSize);

                canvas.drawText(label.substring(0, label.length() - charsToTruncate) + ELLIPSIS,
                        MIN_ICON_TEXT_MARGIN, getHeight() - heightPadding, textPaint);
            } else {
                canvas.drawText(label, (getWidth() - textContainer.width()) / 2f, getHeight() - heightPadding, textPaint);
            }
        }

        // center the icon
        if (icon != null) {
            canvas.save();
            canvas.translate((getWidth() - iconSize) / 2, heightPadding);
            icon.setBounds(0, 0, (int) iconSize, (int) iconSize);
            icon.draw(canvas);
            canvas.restore();
        }
    }

    public float getDrawIconTop() {
        return heightPadding;
    }

    public float getDrawIconLeft() {
        return (getWidth() - iconSize) / 2;
    }

    @Override
    public void onIconAvailable(Drawable drawable, int index) {
        icon = drawable;
        super.invalidate();
    }

    @Override
    public void onIconCleared(Drawable placeholder, int index) {
        icon = placeholder;
        super.invalidate();
    }

    @Override
    public void onAttachedToWindow() {
        if (!fastAdapterItem && iconProvider != null) {
            iconProvider.loadIconIntoIconDrawer(this, (int) iconSize, 0);
        }
        super.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        if (!fastAdapterItem && iconProvider != null) {
            iconProvider.cancelLoad(this);
            icon = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void invalidate() {
        if (!fastAdapterItem && iconProvider != null) {
            iconProvider.cancelLoad(this);
            icon = null;
        } else {
            super.invalidate();
        }
    }

    public interface LongPressCallBack {
        boolean readyForDrag(View view);

        void afterDrag(View view);
    }

    public static class Builder {
        AppItemView view;

        public Builder(Context context, int iconSize) {
            view = new AppItemView(context);
            view.setIconSize(Tool.dp2px(iconSize, view.getContext()));
        }

        public Builder(AppItemView view, int iconSize) {
            this.view = view;
            view.setIconSize(Tool.dp2px(iconSize, view.getContext()));
        }

        public static OnTouchListener getOnTouchGetPosition(Item item, ItemGestureListener.ItemGestureCallback itemGestureCallback) {
            return Tool.getItemOnTouchListener(item, itemGestureCallback);
        }

        public static OnLongClickListener getLongClickDragAppListener(final Item item, final DragAction.Action action, @Nullable final LongPressCallBack eventAction) {
            return new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (Setup.appSettings().isDesktopLock()) {
                        return false;
                    }
                    if (eventAction != null && !eventAction.readyForDrag(v)) {
                        return false;
                    }
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    DragNDropHandler.startDrag(v, item, action, eventAction);
                    return true;
                }
            };
        }

        public AppItemView getView() {
            return view;
        }

        public Builder setAppItem(final App app) {
            view.setLabel(app.getLabel());
            view.setIconProvider(app.getIconProvider());
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Tool.createScaleInScaleOutAnim(view, new Runnable() {
                        @Override
                        public void run() {
                            Tool.startApp(view.getContext(), app, view);
                        }
                    }, 0.85f);
                }
            });
            return this;
        }

        public Builder setAppItem(final Item item, final App app) {
            view.setLabel(item.getLabel());
            view.setIconProvider(app.getIconProvider());
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Tool.createScaleInScaleOutAnim(view, new Runnable() {
                        @Override
                        public void run() {
                            Tool.startApp(view.getContext(), app, view);
                        }
                    }, 0.85f);
                }
            });
            return this;
        }

        public Builder setShortcutItem(final Item item) {
            view.setLabel(item.getLabel());
            view.setIconProvider(item.getIconProvider());
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Tool.createScaleInScaleOutAnim(view, new Runnable() {
                        @Override
                        public void run() {
                            view.getContext().startActivity(item.getIntent());
                        }
                    }, 0.85f);
                }
            });
            return this;
        }

        public Builder setGroupItem(Context context, final DesktopCallBack callback, final Item item, int iconSize) {
            view.setLabel(item.getLabel());
            view.setIconProvider(Setup.imageLoader().createIconProvider(new GroupIconDrawable(context, item, iconSize)));
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Home.Companion.getLauncher() != null && (Home.Companion.getLauncher()).getGroupPopup().showWindowV(item, v, callback)) {
                        ((GroupIconDrawable) ((AppItemView) v).getCurrentIcon()).popUp();
                    }
                }
            });
            return this;
        }

        public Builder setActionItem(Item item) {
            view.setLabel(item.getLabel());
            view.setIconProvider(Setup.imageLoader().createIconProvider(
                    ContextCompat.getDrawable(Setup.appContext(), R.drawable.ic_apps_white_48dp)));
            switch (item.getActionValue()) {
                case Definitions.ACTION_LAUNCHER:
                    view.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                            Home.Companion.getLauncher().openAppDrawer(view);
                        }
                    });
                    break;
            }
            return this;
        }

        public Builder withOnLongClick(final App app, final DragAction.Action action, @Nullable final LongPressCallBack eventAction) {
            withOnLongClick(Item.newAppItem(app), action, eventAction);
            return this;
        }

        public Builder withOnLongClick(final Item item, final DragAction.Action action, @Nullable final LongPressCallBack eventAction) {
            view.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (Setup.appSettings().isDesktopLock()) {
                        return false;
                    }
                    if (eventAction != null && !eventAction.readyForDrag(v)) {
                        return false;
                    }
                    if (view.vibrateWhenLongPress) {
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    }
                    DragNDropHandler.startDrag(view, item, action, eventAction);
                    return true;
                }
            });
            return this;
        }

        public Builder withOnTouchGetPosition(Item item, ItemGestureListener.ItemGestureCallback itemGestureCallback) {
            view.setOnTouchListener(Tool.getItemOnTouchListener(item, itemGestureCallback));
            return this;
        }

        public Builder setTextColor(@ColorInt int color) {
            view.textPaint.setColor(color);
            return this;
        }

        public Builder setFontSize(Context context, float fontSizeSp) {
            view.textPaint.setTextSize(Tool.sp2px(context, fontSizeSp));
            return this;
        }

        public Builder setLabelVisibility(boolean visible) {
            view.showLabel = visible;
            return this;
        }

        public Builder vibrateWhenLongPress() {
            view.vibrateWhenLongPress = true;
            return this;
        }

        public Builder setFastAdapterItem() {
            view.fastAdapterItem = true;
            return this;
        }
    }
}
