package com.forbutton;

public class CandidateControl {
	private CandidateView candidateView;
	private ForButton forButton;
	
	public CandidateControl(ForButton _forButton,CandidateView _candidateView){
		forButton = _forButton;
		candidateView = _candidateView;
		candidateView = new CandidateView(forButton);
		candidateView.setService(forButton);
	}
	
	public CandidateView getCandidateView()
	{
		return candidateView;
	}
	
	public void setTarget(int start_X, int current_X, int start_Y, int current_Y)
	{
		if (current_X - start_X > 0){
			candidateView.setTarget(current_X+28, current_Y);
		} else {
			candidateView.setTarget(current_X, current_Y);
		}
	}
	
}
