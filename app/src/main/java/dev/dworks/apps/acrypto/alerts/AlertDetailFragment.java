package dev.dworks.apps.acrypto.alerts;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.dworks.apps.acrypto.App;
import dev.dworks.apps.acrypto.R;
import dev.dworks.apps.acrypto.common.ActionBarFragment;
import dev.dworks.apps.acrypto.entity.CoinDetailSample;
import dev.dworks.apps.acrypto.entity.CoinDetails;
import dev.dworks.apps.acrypto.entity.Coins;
import dev.dworks.apps.acrypto.entity.Currencies;
import dev.dworks.apps.acrypto.entity.Exchanges;
import dev.dworks.apps.acrypto.entity.PriceAlert;
import dev.dworks.apps.acrypto.misc.FirebaseHelper;
import dev.dworks.apps.acrypto.misc.UrlConstant;
import dev.dworks.apps.acrypto.misc.UrlManager;
import dev.dworks.apps.acrypto.network.GsonRequest;
import dev.dworks.apps.acrypto.network.VolleyPlusHelper;
import dev.dworks.apps.acrypto.settings.SettingsActivity;
import dev.dworks.apps.acrypto.utils.Utils;
import dev.dworks.apps.acrypto.utils.Utils.OnFragmentInteractionListener;
import dev.dworks.apps.acrypto.view.ImageView;
import dev.dworks.apps.acrypto.view.Spinner;

import static dev.dworks.apps.acrypto.entity.Exchanges.ALL_EXCHANGES;
import static dev.dworks.apps.acrypto.entity.Exchanges.NO_EXCHANGES;
import static dev.dworks.apps.acrypto.home.HomeFragment.LIMIT_ALT;
import static dev.dworks.apps.acrypto.settings.SettingsActivity.CONDITION_DEFAULT;
import static dev.dworks.apps.acrypto.settings.SettingsActivity.CURRENCY_FROM_DEFAULT;
import static dev.dworks.apps.acrypto.settings.SettingsActivity.CURRENCY_TO_DEFAULT;
import static dev.dworks.apps.acrypto.settings.SettingsActivity.FREQUENCY_DEFAULT;
import static dev.dworks.apps.acrypto.settings.SettingsActivity.getUserCurrencyFrom;
import static dev.dworks.apps.acrypto.utils.Utils.BUNDLE_ALERT;
import static dev.dworks.apps.acrypto.utils.Utils.BUNDLE_REF_KEY;
import static dev.dworks.apps.acrypto.utils.Utils.getCurrencySymbol;

public class AlertDetailFragment extends ActionBarFragment {

    private static final String TAG = "AlertDetails";
    private static final String REQUIRED = "Required";

    private OnFragmentInteractionListener mListener;
    private PriceAlert mPriceAlert;
    private Spinner mCurrencyFromSpinner;
    private Spinner mCurrencyToSpinner;
    private Spinner mExchangeSpinner;
    private Spinner mFrequencySpinner;
    private Spinner mConditionSpinner;
    private EditText mValue;
    private ImageView mIcon;
    private TextView mSymbol;
    private String refKey;
    private String curencyTo;
    private String curencyFrom;
    private String currencyExchange;
    private String condition;
    private String frequency;
    private double value;
    private int status = 1;

    public static void show(FragmentManager fm, PriceAlert priceAlert, String refKey) {
        final Bundle args = new Bundle();
        args.putSerializable(BUNDLE_ALERT, priceAlert);
        args.putString(BUNDLE_REF_KEY, refKey);
        final FragmentTransaction ft = fm.beginTransaction();
        final AlertDetailFragment fragment = new AlertDetailFragment();
        fragment.setArguments(args);
        ft.replace(R.id.container, fragment, TAG);
        ft.commitAllowingStateLoss();
    }

    public static AlertDetailFragment get(FragmentManager fm) {
        return (AlertDetailFragment) fm.findFragmentByTag(TAG);
    }

    public static void hide(FragmentManager fm){
        if(null != get(fm)){
            fm.beginTransaction().remove(get(fm)).commitAllowingStateLoss();
        }
    }

    public AlertDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPriceAlert = (PriceAlert)getArguments().getSerializable(BUNDLE_ALERT);
        refKey = getArguments().getString(BUNDLE_REF_KEY);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alert_detail, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.detail_toolbar);
        toolbar.setTitle((mPriceAlert == null ? "Add" : "Edit") + " Price Alert");
        toolbar.setNavigationIcon(R.drawable.ic_close);

        getActionBarActivity().setSupportActionBar(toolbar);

        mCurrencyFromSpinner = (Spinner) view.findViewById(R.id.currencyFromSpinner);
        mCurrencyToSpinner = (Spinner) view.findViewById(R.id.currencyToSpinner);
        mExchangeSpinner = (Spinner) view.findViewById(R.id.exchangeSpinner);
        mFrequencySpinner = (Spinner) view.findViewById(R.id.frequencySpinner);
        mConditionSpinner = (Spinner) view.findViewById(R.id.conditionSpinner);
        mValue = (EditText) view.findViewById(R.id.value);
        mSymbol = (TextView) view.findViewById(R.id.symbol);
        mIcon = (ImageView) view.findViewById(R.id.icon);

        setSpinners();

        if(null != mPriceAlert) {
            curencyFrom = getNameArrayt()[0];
            curencyTo = getNameArrayt()[1];
            if(getNameArrayt().length == 3){
                currencyExchange = getNameArrayt()[2];
            }
            value = mPriceAlert.value;
            condition = mPriceAlert.condition;
            frequency = mPriceAlert.frequency;
            status = mPriceAlert.status;

            mValue.setText(String.valueOf(value));
            Utils.setSpinnerValue(mConditionSpinner, "<", getCondition());
            Utils.setSpinnerValue(mFrequencySpinner, "Onetime", getFrequency());
        }
        loadSymbol();
        loadIcon();
    }

    private void loadSymbol() {
        mSymbol.setText(getCurrentCurrencyToSymbol());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        fetchData();
    }

    public void loadIcon(){
        String url = "";
        try {
            final CoinDetailSample.CoinDetail coinDetail =
                    App.getInstance().getCoinDetails().coins.get(getCurrentCurrencyFrom());
            url = Coins.BASE_URL + coinDetail.id + ".png";
        } catch (Exception e){
        }

        mIcon.setImageUrl(url, VolleyPlusHelper.with(getActivity()).getImageLoader());
    }

    private String[] getNameArrayt() {
        return mPriceAlert.name.split("-");
    }

    private void setSpinners() {
        setCurrencyFromSpinner();
        setCurrencyToSpinner();
        setExchangeSpinner();
        setFrequencySpinner();
        setConditionSpinner();
    }

    private void setConditionSpinner() {
        List<String> list = Arrays.asList(getResources().getStringArray(R.array.alert_condition));
        mConditionSpinner.getPopupWindow().setWidth(300);
        mConditionSpinner.setItems(list);
        mConditionSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(Spinner view, int position, long id, String item) {
                condition = item;
            }
        });
    }

    private void setFrequencySpinner() {
        List<String> list = Arrays.asList(getResources().getStringArray(R.array.alert_frequency));
        mFrequencySpinner.getPopupWindow().setWidth(800);
        mFrequencySpinner.setItems(list);
        mFrequencySpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(Spinner view, int position, long id, String item) {
                frequency = item;
            }
        });
    }

    private void setCurrencyFromSpinner() {
        mCurrencyFromSpinner.getPopupWindow().setWidth(300);
        mCurrencyFromSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener<CoinDetails.Coin>() {

            @Override public void onItemSelected(Spinner view, int position, long id, CoinDetails.Coin item) {
                curencyFrom = item.code;
                loadIcon();
                fetchData();
            }
        });
    }

    private void setCurrencyToSpinner() {
        mCurrencyToSpinner.getPopupWindow().setWidth(300);
        mCurrencyToSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener<Currencies.Currency>() {

            @Override public void onItemSelected(Spinner view, int position, long id, Currencies.Currency item) {
                curencyTo = item.code;
                loadSymbol();
                fetchData();
            }
        });
    }

    private void setExchangeSpinner() {
        mExchangeSpinner.setText(SettingsActivity.getExchange());
        mExchangeSpinner.getPopupWindow().setWidth(800);
        mExchangeSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener<Exchanges.Exchange>() {

            @Override
            public void onItemSelected(Spinner var1, int var2, long var3, Exchanges.Exchange exchange) {
                currencyExchange = exchange.exchange;
            }
        });
    }

    private void fetchData() {
        fetchCurrencyFromData();
        fetchCurrencyToData();
        fetchExchangeData();
    }

    private void fetchCurrencyFromData() {
        ArrayMap<String, String> params = new ArrayMap<>();

        String url = UrlManager.with(UrlConstant.COINS_API)
                .setDefaultParams(params).getUrl();

        GsonRequest<Coins> request = new GsonRequest<>(url,
                Coins.class,
                "",
                new Response.Listener<Coins>() {
                    @Override
                    public void onResponse(Coins coins) {
                        mCurrencyFromSpinner.setItems(coins.coins);
                        Utils.setSpinnerValue(mCurrencyFromSpinner, CURRENCY_FROM_DEFAULT, getCurrentCurrencyFrom());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {

                    }
                });
        request.setDontExpireCache();
        request.setShouldCache(true);
        VolleyPlusHelper.with(getActivity()).updateToRequestQueue(request, "currency_from");
    }

    private void fetchCurrencyToData() {
        ArrayMap<String, String> params = new ArrayMap<>();

        String url = UrlManager.with(UrlConstant.CURRENCY_API)
                .setDefaultParams(params).getUrl();

        GsonRequest<Currencies> request = new GsonRequest<>(url,
                Currencies.class,
                "",
                new Response.Listener<Currencies>() {
                    @Override
                    public void onResponse(Currencies currencies) {
                        mCurrencyToSpinner.setItems(getCurrencyToList(currencies.currencies));
                        Utils.setSpinnerValue(mCurrencyToSpinner, (isTopAltCoin() ? CURRENCY_TO_DEFAULT
                                : CURRENCY_FROM_DEFAULT), getCurrentCurrencyTo());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {

                    }
                });
        request.setDontExpireCache();
        request.setShouldCache(true);
        VolleyPlusHelper.with(getActivity()).updateToRequestQueue(request, "currency_to");
    }

    private void fetchExchangeData() {
        ArrayMap<String, String> params = new ArrayMap<>();
        params.put("fsym", getCurrentCurrencyFrom());
        params.put("tsym", getCurrentCurrencyTo());
        params.put("limit", String.valueOf(30));

        String url = UrlManager.with(UrlConstant.EXCHANGELIST_URL)
                .setDefaultParams(params).getUrl();

        GsonRequest<Exchanges> request = new GsonRequest<>(url,
                Exchanges.class,
                "",
                new Response.Listener<Exchanges>() {
                    @Override
                    public void onResponse(Exchanges prices) {
                        mExchangeSpinner.setItems(prices.getAllData());
                        Utils.setSpinnerValue(mExchangeSpinner, ALL_EXCHANGES, getCurrentExchange());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {

                    }
                });
        request.setCacheMinutes(1440, 1440);
        request.setShouldCache(true);
        VolleyPlusHelper.with(getActivity()).updateToRequestQueue(request, "exchange");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.alert_detail, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                break;

            case R.id.action_save:
                saveAlert();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveAlert() {

        if (getValue() == 0) {
            mValue.setError(REQUIRED);
            return;
        }

        if(!Utils.isNetConnected(getActivity())){
            Utils.showNoInternetSnackBar(getActivity(), null);
            return;
        }

        setEditingEnabled(false);
        PriceAlert priceAlert = new PriceAlert(getName(), getCurrentCurrencyFromSymbol(),
                getCurrentCurrencyToSymbol(), status, getCondition(), getFrequency(),
                "value", getValue());
        DatabaseReference reference = FirebaseHelper.getFirebaseDatabaseReference()
                .child("/user_alerts/prices").child(FirebaseHelper.getCurrentUser().getUid());

        DatabaseReference alertReference;
        if(TextUtils.isEmpty(refKey)){
            alertReference = reference.push();
        } else {
            alertReference = reference.child(refKey);
        }
        alertReference.setValue(priceAlert, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                setEditingEnabled(true);
                if(null == databaseError){
                    getActivity().finish();
                }
            }
        });
    }

    public String getCurrentCurrencyTo(){
        return TextUtils.isEmpty(curencyTo)
                ? (isTopAltCoin() ? getUserCurrencyFrom() : CURRENCY_FROM_DEFAULT) : curencyTo;
    }

    public boolean isTopAltCoin(){
        return mCurrencyFromSpinner.getSelectedIndex() <= LIMIT_ALT;
    }

    public String getCurrentCurrencyFrom(){
        return TextUtils.isEmpty(curencyFrom) ? CURRENCY_FROM_DEFAULT : curencyFrom;
    }

    public String getCurrentCurrencyName(){
        return getCurrentCurrencyFrom() + "/" + getCurrentCurrencyTo();
    }

    public String getCurrentCurrencyToSymbol(){
        return getCurrencySymbol(getCurrentCurrencyTo());
    }
    public String getCurrentCurrencyFromSymbol(){
        return getCurrencySymbol(getCurrentCurrencyFrom());
    }

    private String getCurrentExchange() {
        return TextUtils.isEmpty(currencyExchange) ? ALL_EXCHANGES : currencyExchange;
    }

    public String getFrequency() {
        return TextUtils.isEmpty(frequency)? FREQUENCY_DEFAULT : Utils.toTitleCase(frequency);
    }

    public String getCondition() {
        return TextUtils.isEmpty(condition)? CONDITION_DEFAULT : condition;
    }

    public double getValue() {
        try {
            return Double.valueOf(mValue.getText().toString());
        } catch (NumberFormatException e){
            return 0;
        }
    }

    public String getName(){
        return getCurrentCurrencyFrom() + "-"+  getCurrentCurrencyTo() + "-"
                + (getCurrentExchange().equals(ALL_EXCHANGES)
                || getCurrentExchange().equals(NO_EXCHANGES) ? "" : getCurrentExchange());
    }

    private ArrayList<Currencies.Currency> getCurrencyToList(ArrayList<Currencies.Currency> currencies) {
        ArrayList<Currencies.Currency> list = new ArrayList<>();
        if(!getCurrentCurrencyFrom().equals(CURRENCY_FROM_DEFAULT)){
            if(isTopAltCoin()){
                list.addAll(currencies);
                list.add(new Currencies.Currency(CURRENCY_FROM_DEFAULT));
            } else {
                list.add(new Currencies.Currency(CURRENCY_FROM_DEFAULT));
                list.addAll(currencies);
            }
        } else {
            list.addAll(currencies);
        }
        return list;
    }

    private void setEditingEnabled(boolean enabled) {
        mConditionSpinner.setEnabled(enabled);
        mFrequencySpinner.setEnabled(enabled);
        mCurrencyFromSpinner.setEnabled(enabled);
        mCurrencyToSpinner.setEnabled(enabled);
        mExchangeSpinner.setEnabled(enabled);
        mValue.setEnabled(enabled);
    }
}
