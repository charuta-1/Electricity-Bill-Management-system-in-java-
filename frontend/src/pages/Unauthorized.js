import { Link } from 'react-router-dom';

const Unauthorized = () => (
  <div className="container py-5">
    <div className="row justify-content-center">
      <div className="col-md-7">
        <div className="card card-shadow text-center">
          <div className="card-body p-5">
            <h2 className="mb-3">Access Denied</h2>
            <p className="text-muted mb-4">
              You do not have permission to view this page. Please contact the administrator if you believe this is an error.
            </p>
            <Link className="btn btn-primary" to="/login">
              Back to Login
            </Link>
          </div>
        </div>
      </div>
    </div>
  </div>
);

export default Unauthorized;
