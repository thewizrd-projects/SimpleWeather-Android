package com.thewizrd.shared_resources.controls;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.StringUtils;

public class LocationQuery extends ConstraintLayout {
    private TextView locationNameView;
    private TextView locationCountryView;
    private TextView pinIcon;

    public LocationQuery(Context context) {
        super(context);
        initialize(context);
    }

    public LocationQuery(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public LocationQuery(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View viewLayout = inflater.inflate(R.layout.location_query_view, this);

        viewLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (context.getResources().getConfiguration().uiMode == Configuration.UI_MODE_TYPE_WATCH) {
            setBackground(null);
        } else {
            setBackgroundColor(Colors.WHITE);
        }

        int horizPadding = context.getResources().getDimensionPixelSize(R.dimen.list_horizontal_padding);
        int vertPadding = context.getResources().getDimensionPixelSize(R.dimen.list_vertical_padding);
        ViewCompat.setPaddingRelative(this, horizPadding, vertPadding, horizPadding, vertPadding);

        locationNameView = viewLayout.findViewById(R.id.location_name);
        locationCountryView = viewLayout.findViewById(R.id.location_country);
        pinIcon = viewLayout.findViewById(R.id.pin);
    }

    public void setLocation(LocationQueryViewModel view) {
        locationNameView.setText(view.getLocationName());
        locationCountryView.setText(view.getLocationCountry());
        if (pinIcon != null) {
            pinIcon.setVisibility(
                    StringUtils.isNullOrWhitespace(view.getLocationQuery()) && StringUtils.isNullOrWhitespace(view.getLocationCountry()) ?
                            View.INVISIBLE : View.VISIBLE);
        }
    }
}
