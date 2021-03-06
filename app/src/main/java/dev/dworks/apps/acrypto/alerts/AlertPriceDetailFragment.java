package dev.dworks.apps.acrypto.alerts;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.android.volley.request.StringRequest;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import org.fabiomsr.moneytextview.MoneyTextView;
import org.json.JSONException;
import org.json.JSONObject;

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
import dev.dworks.apps.acrypto.entity.AlertPrice;
import dev.dworks.apps.acrypto.misc.AnalyticsManager;
import dev.dworks.apps.acrypto.misc.FirebaseHelper;
import dev.dworks.apps.acrypto.misc.UrlConstant;
import dev.dworks.apps.acrypto.misc.UrlManager;
import dev.dworks.apps.acrypto.network.GsonRequest;
import dev.dworks.apps.acrypto.network.MasterGsonRequest;
import dev.dworks.apps.acrypto.network.VolleyPlusHelper;
import dev.dworks.apps.acrypto.network.VolleyPlusMasterHelper;
import dev.dworks.apps.acrypto.utils.Utils;
import dev.dworks.apps.acrypto.utils.Utils.OnFragmentInteractionListener;
import dev.dworks.apps.acrypto.view.ImageView;
import dev.dworks.apps.acrypto.view.SearchableSpinner;
import dev.dworks.apps.acrypto.view.SimpleSpinner;

import static android.view.View.GONE;
import static dev.dworks.apps.acrypto.entity.Exchanges.ALL_EXCHANGES;
import static dev.dworks.apps.acrypto.entity.Exchanges.NO_EXCHANGES;
import static dev.dworks.apps.acrypto.settings.SettingsActivity.CONDITION_DEFAULT;
import static dev.dworks.apps.acrypto.settings.SettingsActivity.CURRENCY_FROM_DEFAULT;
import static dev.dworks.apps.acrypto.settings.SettingsActivity.FREQUENCY_DEFAULT;
import static dev.dworks.apps.acrypto.settings.SettingsActivity.getCurrencyToKey;
import static dev.dworks.apps.acrypto.settings.SettingsActivity.getUserCurrencyFrom;
import static dev.dworks.apps.acrypto.utils.Utils.BUNDLE_ALERT;
import static dev.dworks.apps.acrypto.utils.Utils.BUNDLE_REF_KEY;
import static dev.dworks.apps.acrypto.utils.Utils.getCurrencySymbol;
import static dev.dworks.apps.acrypto.utils.Utils.getDecimalFormat;

public class AlertPriceDetailFragment extends ActionBarFragment
        implements Response.Listener<String>, Response.ErrorListener, AdapterView.OnItemSelectedListener  {

    private static final String TAG = "AlertPriceDetails";
    private static final String REQUIRED = "Required";

    private OnFragmentInteractionListener mListener;
    private AlertPrice mAlertPrice;
    private SearchableSpinner mCurrencyFromSpinner;
    private SearchableSpinner mCurrencyToSpinner;
    private SearchableSpinner mExchangeSpinner;
    private SimpleSpinner mFrequencySpinner;
    private SimpleSpinner mConditionSpinner;
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
    private View mProgress;
    private MoneyTextView mCurrentValue;
    private View mPriceProgress;
    private TextView mMessage;
    private boolean canLoadValue = true;
    private TextView mNotes;
    private String notes;

    public static void show(FragmentManager fm, AlertPrice alertPrice, String refKey) {
        final Bundle args = new Bundle();
        args.putSerializable(BUNDLE_ALERT, alertPrice);
        args.putString(BUNDLE_REF_KEY, refKey);
        final FragmentTransaction ft = fm.beginTransaction();
        final AlertPriceDetailFragment fragment = new AlertPriceDetailFragment();
        fragment.setArguments(args);
        ft.replace(R.id.container, fragment, TAG);
        ft.commitAllowingStateLoss();
    }

    public static AlertPriceDetailFragment get(FragmentManager fm) {
        return (AlertPriceDetailFragment) fm.findFragmentByTag(TAG);
    }

    public static void hide(FragmentManager fm){
        if(null != get(fm)){
            fm.beginTransaction().remove(get(fm)).commitAllowingStateLoss();
        }
    }

    public AlertPriceDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAlertPrice = (AlertPrice)getArguments().getSerializable(BUNDLE_ALERT);
        refKey = getArguments().getString(BUNDLE_REF_KEY);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alert_price_detail, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.detail_toolbar);
        toolbar.setTitle((!isEdit() ? "Add" : "Edit") + " Price Alert");
        toolbar.setNavigationIcon(R.drawable.ic_close);

        getActionBarActivity().setSupportActionBar(toolbar);

        mProgress = view.findViewById(R.id.progress);
        mPriceProgress = view.findViewById(R.id.priceprogress);
        mCurrencyFromSpinner = (SearchableSpinner) view.findViewById(R.id.currencyFromSpinner);
        mCurrencyToSpinner = (SearchableSpinner) view.findViewById(R.id.currencyToSpinner);
        mExchangeSpinner = (SearchableSpinner) view.findViewById(R.id.exchangeSpinner);
        mFrequencySpinner = (SimpleSpinner) view.findViewById(R.id.frequencySpinner);
        mConditionSpinner = (SimpleSpinner) view.findViewById(R.id.conditionSpinner);
        mValue = (EditText) view.findViewById(R.id.value);
        mSymbol = (TextView) view.findViewById(R.id.symbol);
        mIcon = (ImageView) view.findViewById(R.id.icon);
        mCurrentValue = (MoneyTextView) view.findViewById(R.id.currentValue);
        mMessage = (TextView) view.findViewById(R.id.message);
        mNotes = (TextView) view.findViewById(R.id.notes);
        setSpinners();

        if(null != mAlertPrice) {
            curencyFrom = getNameArrayt()[0];
            curencyTo = getNameArrayt()[1];
            if(getNameArrayt().length == 3){
                currencyExchange = getNameArrayt()[2];
            }
            value = mAlertPrice.value;
            condition = mAlertPrice.condition;
            frequency = mAlertPrice.frequency;
            status = mAlertPrice.status;
            notes = mAlertPrice.notes;

            canLoadValue = TextUtils.isEmpty(refKey);
            loadValue(true);
            mNotes.setText(notes);
            mFrequencySpinner.setSelection(getFrequency());
            mConditionSpinner.setSelection(getCondition());
        }
        loadSymbol();
        loadIcon();
        fetchCurrentPriceData();
    }

    private void loadValue(boolean forceLoad) {
        if(!forceLoad && !canLoadValue) {
            canLoadValue = true;
            return;
        }
        mValue.setText(String.valueOf(getDecimalFormat(getCurrentCurrencyToSymbol()).format(value)));
    }

    private boolean isEdit() {
        return mAlertPrice != null && !TextUtils.isEmpty(refKey);
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
        return mAlertPrice.name.split("-");
    }

    private void setSpinners() {
        mCurrencyFromSpinner.setOnItemSelectedListener(this);
        mCurrencyToSpinner.setOnItemSelectedListener(this);
        mExchangeSpinner.setOnItemSelectedListener(this);

        List<String> frequencyList = Arrays.asList(getResources().getStringArray(R.array.alert_frequency));
        mFrequencySpinner.setItems(new ArrayList(frequencyList));
        mFrequencySpinner.setOnItemSelectedListener(this);
        List<String> conditionList = Arrays.asList(getResources().getStringArray(R.array.alert_condition));
        mConditionSpinner.setItems(new ArrayList(conditionList));
        mConditionSpinner.setOnItemSelectedListener(this);
    }

    @Override
    protected void fetchData() {
        fetchCurrencyFromData();
        fetchCurrencyToData();
        fetchExchangeData();
        fetchCurrentPriceData();
    }

    public String getUrl(){
        ArrayMap<String, String> params = new ArrayMap<>();
        params.put("fsym", getCurrentCurrencyFrom());
        params.put("tsyms", getCurrentCurrencyTo());

        String url = UrlManager.with(UrlConstant.HISTORY_PRICE_URL)
                .setDefaultParams(params).getUrl();
        return url;
    }

    private void fetchCurrentPriceData() {
        mPriceProgress.setVisibility(View.VISIBLE);
        StringRequest request = new StringRequest(getUrl(), this,
                this);
        VolleyPlusHelper.with(getActivity()).updateToRequestQueue(request, "diff");
    }

    private void fetchCurrencyFromData() {

        String url = UrlManager.with(UrlConstant.COINS_API).getUrl();

        MasterGsonRequest<Coins> request = new MasterGsonRequest<>(url,
                Coins.class,
                new Response.Listener<Coins>() {
                    @Override
                    public void onResponse(Coins coins) {
                        mCurrencyFromSpinner.setItems(coins.coins);
                        mCurrencyFromSpinner.setSelection(getCurrentCurrencyFrom());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {

                    }
                });
        request.setMasterExpireCache();
        request.setShouldCache(true);
        VolleyPlusMasterHelper.with(getActivity()).updateToRequestQueue(request, "currency_from");
    }

    private void fetchCurrencyToData() {
        String url = UrlManager.with(UrlConstant.CURRENCY_API).getUrl();

        MasterGsonRequest<Currencies> request = new MasterGsonRequest<>(url,
                Currencies.class,
                new Response.Listener<Currencies>() {
                    @Override
                    public void onResponse(Currencies currencies) {
                        mCurrencyToSpinner.setItems(getCurrencyToList(currencies.currencies));
                        mCurrencyToSpinner.setSelection(getCurrentCurrencyTo());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {

                    }
                });
        request.setMasterExpireCache();
        request.setShouldCache(true);
        VolleyPlusMasterHelper.with(getActivity()).updateToRequestQueue(request, "currency_to");
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
                        mExchangeSpinner.setSelection(getCurrentExchange());
                        if(prices.getAllData().size() == 1
                                && prices.getAllData().get(0).toString().equals(NO_EXCHANGES)){
                            mExchangeSpinner.setEnabled(false);
                        }else {
                            mExchangeSpinner.setEnabled(true);
                        }
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
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_delete).setVisible(isEdit());
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                AnalyticsManager.logEvent("alert_edit_cancelled");
                getActivity().finish();
                break;

            case R.id.action_save:
                saveAlert();
                break;

            case R.id.action_delete:
                deleteAlert();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteAlert() {
        if(!Utils.isNetConnected(getActivity())){
            Utils.showNoInternetSnackBar(getActivity(), null);
            return;
        }

        setEditingEnabled(false);
        DatabaseReference reference = FirebaseHelper.getFirebaseDatabaseReference()
                .child("/user_alerts/price")
                .child(FirebaseHelper.getCurrentUid())
                .child(refKey);
        reference.removeValue(new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                setEditingEnabled(true);
                if(null == databaseError && null != getActivity()){
                    AnalyticsManager.logEvent("alert_deleted");
                    getActivity().finish();
                }
            }
        });
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
        AlertPrice alertPrice = new AlertPrice(getName(), getCurrentCurrencyFromSymbol(),
                getCurrentCurrencyToSymbol(), status, getCondition(), getFrequency(),
                "value", getNotes(), getValue());
        DatabaseReference reference = FirebaseHelper.getFirebaseDatabaseReference()
                .child("/user_alerts/price").child(FirebaseHelper.getCurrentUid());

        DatabaseReference alertReference;
        if(TextUtils.isEmpty(refKey)){
            alertReference = reference.push();
        } else {
            alertReference = reference.child(refKey);
        }
        alertReference.setValue(alertPrice, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                setEditingEnabled(true);
                if(null == databaseError && null != getActivity()){
                    AnalyticsManager.logEvent(TextUtils.isEmpty(refKey) ? "alert_added" : "alert_edited");
                    getActivity().finish();
                }
            }
        });
    }

    public String getCurrentCurrencyTo(){
        return TextUtils.isEmpty(curencyTo) ? getDefaultCurrencyTo() : curencyTo;
    }

    private String getDefaultCurrencyTo(){
        return PreferenceManager.getDefaultSharedPreferences(App.getInstance().getBaseContext())
                .getString(getCurrencyToKey(), isTopAltCoin() ? getUserCurrencyFrom() : CURRENCY_FROM_DEFAULT);
    }

    public boolean isTopAltCoin(){
        return App.getInstance().getDefaultData().coins_top.contains(getCurrentCurrencyFrom());
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

    public String getNotes() {
        return mNotes.getText().toString();
    }

    private void setEditingEnabled(boolean enabled) {
        mConditionSpinner.setEnabled(enabled);
        mFrequencySpinner.setEnabled(enabled);
        mCurrencyFromSpinner.setEnabled(enabled);
        mCurrencyToSpinner.setEnabled(enabled);
        mExchangeSpinner.setEnabled(enabled);
        mValue.setEnabled(enabled);
        mNotes.setEnabled(enabled);
        mProgress.setVisibility(enabled ? GONE : View.VISIBLE);
    }

    private void hideKeyboard() {
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    private void showKeyboard(EditText et) {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
    }

    @Override
    public void onErrorResponse(VolleyError volleyError) {
        mPriceProgress.setVisibility(GONE);
        if(!Utils.isActivityAlive(getActivity())){
            return;
        }
        if (!Utils.isNetConnected(getActivity())) {
            setEmptyData("No Internet");
            Utils.showNoInternetSnackBar(getActivity(), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fetchCurrentPriceData();
                }
            });
        }
        else{
            setEmptyData("Cant Connect to ACrypto");
            Utils.showRetrySnackBar(getActivity(), "Cant Connect to Acrypto", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fetchCurrentPriceData();
                }
            });
        }
    }

    @Override
    protected void setEmptyData(String message) {
        Utils.setPriceValue(mCurrentValue, 0, getCurrentCurrencyToSymbol());
        mMessage.setText(message);
        value = 0;
        loadValue(false);
    }

    @Override
    public void onResponse(String response) {
        mPriceProgress.setVisibility(GONE);
        try {
            JSONObject jsonObject = new JSONObject(response);
            double currentPrice = jsonObject.getDouble(getCurrentCurrencyTo());
            Utils.setPriceValue(mCurrentValue, currentPrice, getCurrentCurrencyToSymbol());
            value = currentPrice;
            loadValue(false);
        } catch (JSONException e) {
            e.printStackTrace();
            setEmptyData("Something went wrong!");
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Bundle bundle = new Bundle();
        switch (parent.getId()){
            case R.id.currencyFromSpinner:
                CoinDetails.Coin coin = (CoinDetails.Coin) parent.getSelectedItem();
                curencyFrom = coin.code;
                currencyExchange = "";
                loadIcon();
                fetchData();
                break;
            case R.id.currencyToSpinner:
                Currencies.Currency currency = (Currencies.Currency) parent.getSelectedItem();
                curencyTo = currency.code;
                currencyExchange = "";
                loadSymbol();
                fetchData();
                break;
            case R.id.exchangeSpinner:
                Exchanges.Exchange exchange = (Exchanges.Exchange) parent.getSelectedItem();
                currencyExchange = exchange.exchange;
                break;
            case R.id.frequencySpinner:
                frequency = (String) parent.getSelectedItem();
                break;
            case R.id.conditionSpinner:
                condition = (String) parent.getSelectedItem();
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
