package com.fiuba.tdp.petadopt.fragments.detail.questions;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fiuba.tdp.petadopt.R;
import com.fiuba.tdp.petadopt.model.Question;
import com.fiuba.tdp.petadopt.util.DateUtils;

import java.util.List;


public class QAListItemAdapter extends ArrayAdapter<Question> {
    private final List<Question> questions;
    private final Activity context;


    public QAListItemAdapter(Activity context,
                             List<Question> questions) {
        super(context, R.layout.pet_list_item, questions);
        this.context = context;
        this.questions = questions;

    }


    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.qa_list_item, null, true);

        TextView questionTextView = (TextView) rowView.findViewById(R.id.question);
        TextView questionDateTextView = (TextView) rowView.findViewById(R.id.question_date);
        TextView questionAskerTextView = (TextView) rowView.findViewById(R.id.asker_name);
        TextView answerTextView = (TextView) rowView.findViewById(R.id.answer);
        TextView answerDateTextView = (TextView) rowView.findViewById(R.id.answer_date);
        Button answerButton = (Button) rowView.findViewById(R.id.answer_question_button);

        RelativeLayout relativeLayout = (RelativeLayout) rowView.findViewById(R.id.relative_layout);

        Question question = questions.get(position);
        questionTextView.setText(question.getText());
        questionDateTextView.setText(DateUtils.stringFromDateForQuestionList(question.getCreatedAt()));
        questionAskerTextView.setText(question.getAsker());

        if (question.getAnswer() == null) {
            relativeLayout.removeView(answerDateTextView);
            relativeLayout.removeView(answerTextView);
        } else {
            answerTextView.setText(question.getAnswer().getText());
            answerDateTextView.setText(DateUtils.stringFromDateForQuestionList(question.getAnswer().getCreatedAt()));
            relativeLayout.removeView(answerButton);
        }

        relativeLayout.removeView(answerButton); //See line below!
        /* TODO - Show answer question if its the pet owner
         if (isOwner()){
             answerButton.setVisibility(View.VISIBLE);
         }
          //Set button listener!
         */

        return rowView;
    }
}
