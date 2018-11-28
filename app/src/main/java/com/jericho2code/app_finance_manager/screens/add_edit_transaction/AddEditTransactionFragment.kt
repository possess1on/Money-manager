package com.jericho2code.app_finance_manager.screens.add_edit_transaction

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.jericho2code.app_finance_manager.R
import com.jericho2code.app_finance_manager.application.di.owners.ApplicationComponentOwner
import com.jericho2code.app_finance_manager.application.extensions.*
import com.jericho2code.app_finance_manager.model.entity.Template
import com.jericho2code.app_finance_manager.model.entity.TemplateFullObject
import com.jericho2code.app_finance_manager.model.entity.Transaction
import com.jericho2code.app_finance_manager.model.entity.TransactionType
import io.reactivex.Single
import kotlinx.android.synthetic.main.fragment_add_edit_transaction.*
import kotlinx.android.synthetic.main.view_toolbar.*
import kotlinx.android.synthetic.main.view_transaction_save_as_template_item.*
import org.threeten.bp.LocalDateTime

class AddEditTransactionFragment : Fragment() {

    lateinit var viewModel: AddEditTransactionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(activity!!).get(AddEditTransactionViewModel::class.java)
        (activity?.application  as? ApplicationComponentOwner)
            ?.applicationComponent()
            ?.plusTransactionAddEditComponent()
            ?.inject(viewModel)
        viewModel.categoryLiveData.observe(this, Observer { category ->
            transition_category_input.setText(category?.title ?: "")
            category?.let {
                when (it.baseTransactionType) {
                    TransactionType.SPENDING_TRANSACTION -> spending.isChecked = true
                    TransactionType.PROFIT_TRANSACTION -> profit.isChecked = true
                    else -> spending.isChecked = true
                }
            } ?: run {
                spending.isChecked = true
            }
        })
        viewModel.transactionDateLiveData.observe(this, Observer {
            it?.let { setTransactionDateText(it) }
        })
        viewModel.templateDateLiveData.observe(this, Observer { template ->
            template?.run {
                viewModel.setCategory(category.first())
                transaction.first().let {
                    transition_sum_input.setText(it.value.toString())
                    transition_title_input.setText(it.title)
                    transition_description_input.setText(it.description)
                    viewModel.setAsTemplateVisibility(false)
                }
            }
        })
        viewModel.saveAsTemplateVisibilityLiveData.observe(this, Observer { isVisible ->
            transaction_save_as_template_field.visibleOrGone(isVisible ?: true)
        })
        viewModel.setTransactionDate(LocalDateTime.now())
        val template = arguments?.getParcelable<TemplateFullObject>(TEMPLATE)
        template?.let { viewModel.setTemplate(it) }
        viewModel.setAsTemplateVisibility(template == null)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_add_edit_transaction, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationIcon(R.drawable.ic_close)
        toolbar.setTitle(R.string.add_transaction)
        toolbar.inflateMenu(R.menu.save_transaction)
        toolbar.setNavigationOnClickListener {
            context?.hideKeyboard(this.view!!)
            findNavController().navigateUp()
        }
        toolbar.menu.findItem(R.id.save_changes).setOnMenuItemClickListener {
            context?.hideKeyboard(this.view!!)
            viewModel.saveTransaction(
                Transaction(
                    value = transition_sum_input.text.toString().toDoubleOrNull() ?: 0.0,
                    title = transition_title_input.text.toString(),
                    description = transition_description_input.text.toString(),
                    date = viewModel.transactionDateLiveData.value ?: LocalDateTime.now(),
                    transactionType = when (operation_type_group.checkedChipId) {
                        R.id.spending -> TransactionType.SPENDING_TRANSACTION
                        R.id.profit -> TransactionType.PROFIT_TRANSACTION
                        else -> TransactionType.SPENDING_TRANSACTION
                    },
                    categoryId = viewModel.categoryLiveData.value?.id ?: 0
                )
            ).flatMap { transitionId ->
                if (transaction_save_as_template_checkbox.isChecked) {
                    viewModel.saveTemplate(
                        Template(
                            usageCount = 1,
                            transactionId = transitionId,
                            categoryId = viewModel.categoryLiveData.value?.id ?: 0
                        )
                    )
                } else {
                    Single.just(transitionId)
                }
            }.subscribe(
                {
                    context?.showToast(R.string.transaction_saved)
                    findNavController().popBackStack(R.id.transactionListFragment, false)
                },
                {
                    Snackbar.make(view, it.localizedMessage, Snackbar.LENGTH_SHORT).show()
                }
            )
            true
        }
        date_selector_input.setOnClickListener {
            val transactionDate = viewModel.transactionDateLiveData.value ?: LocalDateTime.now()
            val datePickerDialog = TransactionDatePickerDialog.instance(transactionDate.toLocalDate())
            datePickerDialog.show(childFragmentManager, datePickerDialog.tag)
        }
        transition_category_input.setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.action_addEditTransactionFragment_to_selectCategoryFragment)
        )
        viewModel.transactionDateLiveData.value?.let { setTransactionDateText(it) }
    }

    @SuppressLint("SetTextI18n")
    private fun setTransactionDateText(date: LocalDateTime) {
        val todayText = if (date.isToday()) context?.str(R.string.today) + ", " else ""
        date_selector_input.setText(todayText + date.toFullDateString())
    }

    override fun onDetach() {
        super.onDetach()
        viewModel.categoryLiveData.value = null
        viewModel.transactionDateLiveData.value = null
        viewModel.templateDateLiveData.value = null
        viewModel.saveAsTemplateVisibilityLiveData.value = null
    }

    override fun onPause() {
        super.onPause()
        context?.hideKeyboard(view!!)
    }

    companion object {

        const val TEMPLATE = "template"

        fun createArgs(template: TemplateFullObject) = Bundle().apply {
            putParcelable(TEMPLATE, template)
        }
    }
}