import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MockComponent} from "ng-mocks";
import {LoginDetailsComponent} from "./login-details/login-details.component";

import {SettingsComponent} from './settings.component';

describe('SettingsComponent', () => {
    let component: SettingsComponent;
    let fixture: ComponentFixture<SettingsComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [SettingsComponent],
            declarations: [MockComponent(LoginDetailsComponent)]
        })
            .compileComponents();

        fixture = TestBed.createComponent(SettingsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
